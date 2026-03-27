package com.hackathon.finance.service;

import com.hackathon.finance.dto.account.AccountRequest;
import com.hackathon.finance.dto.account.AccountResponse;
import com.hackathon.finance.dto.account.TransferRequest;
import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.exception.BadRequestException;
import com.hackathon.finance.exception.NotFoundException;
import com.hackathon.finance.mapper.EntityMapper;
import com.hackathon.finance.repository.AccountRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserContextService userContextService;
    private final AccountAccessService accountAccessService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts() {
        return accountAccessService.getAccessibleAccounts().stream()
                .map(account -> mapper.toAccountResponse(
                        account,
                        accountAccessService.roleFor(account),
                        account.getUser().getDisplayName(),
                        accountAccessService.getExplicitMembers(account).size() + 1
                ))
                .toList();
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        UserEntity user = userContextService.getCurrentUser();
        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setOpeningBalance(request.openingBalance());
        account.setCurrentBalance(request.openingBalance());
        account.setInstitutionName(request.institutionName());
        AccountEntity savedAccount = accountRepository.save(account);
        return mapper.toAccountResponse(savedAccount, accountAccessService.roleFor(savedAccount), user.getDisplayName(), 1);
    }

    @Transactional
    public AccountResponse update(UUID id, AccountRequest request) {
        AccountEntity account = accountAccessService.findOwned(id);
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setInstitutionName(request.institutionName());
        return mapper.toAccountResponse(account, accountAccessService.roleFor(account), account.getUser().getDisplayName(),
                accountAccessService.getExplicitMembers(account).size() + 1);
    }

    @Transactional
    public void transfer(TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new BadRequestException("Transfer accounts must be different.");
        }
        AccountEntity from = accountAccessService.findAccessible(request.fromAccountId());
        AccountEntity to = accountAccessService.findAccessible(request.toAccountId());
        accountAccessService.ensureEditor(from);
        accountAccessService.ensureEditor(to);
        if (from.getCurrentBalance().compareTo(request.amount()) < 0) {
            throw new BadRequestException("Insufficient funds for transfer.");
        }
        from.setCurrentBalance(from.getCurrentBalance().subtract(request.amount()));
        to.setCurrentBalance(to.getCurrentBalance().add(request.amount()));
    }

    @Transactional(readOnly = true)
    public AccountEntity findAccessible(UUID id) {
        return accountAccessService.findAccessible(id);
    }

    @Transactional(readOnly = true)
    public AccountEntity findOwned(UUID id) {
        return accountAccessService.findOwned(id);
    }
}
