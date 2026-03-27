package com.hackathon.finance.dto.account;

import com.hackathon.finance.entity.enums.AccountMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountMemberRequest(
        @NotBlank @Email String email,
        @NotNull AccountMemberRole role
) {
}
