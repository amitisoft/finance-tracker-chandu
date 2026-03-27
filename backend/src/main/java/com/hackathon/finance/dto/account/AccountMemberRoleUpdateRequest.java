package com.hackathon.finance.dto.account;

import com.hackathon.finance.entity.enums.AccountMemberRole;
import jakarta.validation.constraints.NotNull;

public record AccountMemberRoleUpdateRequest(@NotNull AccountMemberRole role) {
}
