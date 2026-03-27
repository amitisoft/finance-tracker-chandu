package com.hackathon.finance.service;

import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.AccountMemberEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.entity.enums.AccountMemberRole;
import com.hackathon.finance.exception.NotFoundException;
import com.hackathon.finance.exception.UnauthorizedException;
import com.hackathon.finance.repository.AccountMemberRepository;
import com.hackathon.finance.repository.AccountRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountAccessService {

    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final UserContextService userContextService;

    @Transactional(readOnly = true)
    public List<AccountEntity> getAccessibleAccounts() {
        return accountRepository.findAllAccessibleByUser(userContextService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public Set<UUID> getAccessibleAccountIds() {
        return getAccessibleAccounts().stream().map(AccountEntity::getId).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public AccountEntity findAccessible(UUID accountId) {
        UserEntity currentUser = userContextService.getCurrentUser();
        return accountRepository.findById(accountId)
                .filter(account -> hasAccess(account, currentUser))
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }

    @Transactional(readOnly = true)
    public AccountEntity findOwned(UUID accountId) {
        return accountRepository.findByIdAndUser(accountId, userContextService.getCurrentUser())
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }

    @Transactional(readOnly = true)
    public AccountMemberRole roleFor(AccountEntity account) {
        UserEntity currentUser = userContextService.getCurrentUser();
        if (account.getUser().getId().equals(currentUser.getId())) {
            return AccountMemberRole.OWNER;
        }
        return accountMemberRepository.findByAccountAndUser(account, currentUser)
                .map(AccountMemberEntity::getRole)
                .orElseThrow(() -> new UnauthorizedException("You do not have access to this account."));
    }

    @Transactional(readOnly = true)
    public void ensureEditor(AccountEntity account) {
        AccountMemberRole role = roleFor(account);
        if (role == AccountMemberRole.VIEWER) {
            throw new UnauthorizedException("You only have viewer access to this account.");
        }
    }

    @Transactional(readOnly = true)
    public void ensureOwner(AccountEntity account) {
        if (roleFor(account) != AccountMemberRole.OWNER) {
            throw new UnauthorizedException("Only the account owner can perform this action.");
        }
    }

    @Transactional(readOnly = true)
    public List<AccountMemberEntity> getExplicitMembers(AccountEntity account) {
        return accountMemberRepository.findAllByAccountOrderByCreatedAtAsc(account);
    }

    @Transactional(readOnly = true)
    public Map<UUID, AccountMemberRole> roleMapForAccessibleAccounts() {
        Map<UUID, AccountMemberRole> roles = new LinkedHashMap<>();
        for (AccountEntity account : getAccessibleAccounts()) {
            roles.put(account.getId(), roleFor(account));
        }
        return roles;
    }

    private boolean hasAccess(AccountEntity account, UserEntity currentUser) {
        return account.getUser().getId().equals(currentUser.getId())
                || accountMemberRepository.existsByAccountAndUser(account, currentUser);
    }
}
