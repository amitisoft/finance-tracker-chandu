package com.hackathon.finance.dto.account;

import com.hackathon.finance.entity.enums.AccountMemberRole;
import java.util.UUID;

public record AccountMemberResponse(
        UUID userId,
        String email,
        String displayName,
        AccountMemberRole role,
        boolean owner
) {
}
