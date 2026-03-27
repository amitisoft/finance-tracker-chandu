package com.hackathon.finance.service;

import com.hackathon.finance.dto.account.AccountMemberRequest;
import com.hackathon.finance.dto.account.AccountMemberResponse;
import com.hackathon.finance.dto.account.AccountMemberRoleUpdateRequest;
import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.AccountMemberEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.entity.enums.AccountMemberRole;
import com.hackathon.finance.exception.ConflictException;
import com.hackathon.finance.exception.NotFoundException;
import com.hackathon.finance.repository.AccountMemberRepository;
import com.hackathon.finance.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SharedAccountService {

    private final AccountAccessService accountAccessService;
    private final AccountMemberRepository accountMemberRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;

    @Transactional
    public AccountMemberResponse invite(UUID accountId, AccountMemberRequest request) {
        AccountEntity account = accountAccessService.findOwned(accountId);
        accountAccessService.ensureOwner(account);
        UserEntity invitedUser = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new NotFoundException("User with this email was not found."));
        if (invitedUser.getId().equals(account.getUser().getId())) {
            throw new ConflictException("The account owner is already part of this account.");
        }
        accountMemberRepository.findByAccountAndUser(account, invitedUser).ifPresent(existing -> {
            throw new ConflictException("This user is already a member of the account.");
        });
        AccountMemberEntity membership = new AccountMemberEntity();
        membership.setAccount(account);
        membership.setUser(invitedUser);
        membership.setInvitedBy(userContextService.getCurrentUser());
        membership.setRole(request.role());
        accountMemberRepository.save(membership);
        return toResponse(invitedUser, request.role(), false);
    }

    @Transactional(readOnly = true)
    public List<AccountMemberResponse> getMembers(UUID accountId) {
        AccountEntity account = accountAccessService.findAccessible(accountId);
        List<AccountMemberResponse> members = new ArrayList<>();
        members.add(toResponse(account.getUser(), AccountMemberRole.OWNER, true));
        accountAccessService.getExplicitMembers(account).forEach(member ->
                members.add(toResponse(member.getUser(), member.getRole(), false)));
        return members;
    }

    @Transactional
    public AccountMemberResponse updateRole(UUID accountId, UUID memberUserId, AccountMemberRoleUpdateRequest request) {
        AccountEntity account = accountAccessService.findOwned(accountId);
        accountAccessService.ensureOwner(account);
        AccountMemberEntity membership = accountMemberRepository.findByAccountAndUser(account,
                        userRepository.findById(memberUserId).orElseThrow(() -> new NotFoundException("User not found.")))
                .orElseThrow(() -> new NotFoundException("Account member not found."));
        membership.setRole(request.role());
        return toResponse(membership.getUser(), membership.getRole(), false);
    }

    private AccountMemberResponse toResponse(UserEntity user, AccountMemberRole role, boolean owner) {
        return new AccountMemberResponse(user.getId(), user.getEmail(), user.getDisplayName(), role, owner);
    }
}
