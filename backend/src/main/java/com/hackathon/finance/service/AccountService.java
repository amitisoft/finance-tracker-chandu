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
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts() {
        UserEntity user = userContextService.getCurrentUser();
        return accountRepository.findAllByUserOrderByCreatedAtDesc(user).stream().map(mapper::toAccountResponse).toList();
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
        return mapper.toAccountResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponse update(UUID id, AccountRequest request) {
        AccountEntity account = findOwned(id);
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setInstitutionName(request.institutionName());
        return mapper.toAccountResponse(account);
    }

    @Transactional
    public void transfer(TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new BadRequestException("Transfer accounts must be different.");
        }
        AccountEntity from = findOwned(request.fromAccountId());
        AccountEntity to = findOwned(request.toAccountId());
        if (from.getCurrentBalance().compareTo(request.amount()) < 0) {
            throw new BadRequestException("Insufficient funds for transfer.");
        }
        from.setCurrentBalance(from.getCurrentBalance().subtract(request.amount()));
        to.setCurrentBalance(to.getCurrentBalance().add(request.amount()));
    }

    @Transactional(readOnly = true)
    public AccountEntity findOwned(UUID id) {
        return accountRepository.findByIdAndUser(id, userContextService.getCurrentUser())
                .orElseThrow(() -> new NotFoundException("Account not found."));
    }
}
