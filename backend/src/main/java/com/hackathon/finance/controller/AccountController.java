package com.hackathon.finance.controller;

import com.hackathon.finance.dto.account.AccountRequest;
import com.hackathon.finance.dto.account.AccountResponse;
import com.hackathon.finance.dto.account.AccountMemberRequest;
import com.hackathon.finance.dto.account.AccountMemberResponse;
import com.hackathon.finance.dto.account.AccountMemberRoleUpdateRequest;
import com.hackathon.finance.dto.account.TransferRequest;
import com.hackathon.finance.service.AccountService;
import com.hackathon.finance.service.SharedAccountService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final SharedAccountService sharedAccountService;

    @GetMapping
    public List<AccountResponse> getAccounts() {
        return accountService.getAccounts();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return accountService.create(request);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable UUID id, @Valid @RequestBody AccountRequest request) {
        return accountService.update(id, request);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(@Valid @RequestBody TransferRequest request) {
        accountService.transfer(request);
    }

    @PostMapping("/{id}/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountMemberResponse invite(@PathVariable UUID id, @Valid @RequestBody AccountMemberRequest request) {
        return sharedAccountService.invite(id, request);
    }

    @GetMapping("/{id}/members")
    public List<AccountMemberResponse> getMembers(@PathVariable UUID id) {
        return sharedAccountService.getMembers(id);
    }

    @PutMapping("/{id}/members/{userId}")
    public AccountMemberResponse updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody AccountMemberRoleUpdateRequest request
    ) {
        return sharedAccountService.updateRole(id, userId, request);
    }
}
