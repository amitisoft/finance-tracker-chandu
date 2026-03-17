package com.hackathon.finance.dto.account;

import com.hackathon.finance.entity.enums.AccountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        BigDecimal openingBalance,
        BigDecimal currentBalance,
        String institutionName,
        OffsetDateTime updatedAt
) {
}
