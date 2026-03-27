package com.hackathon.finance.dto.transaction;

import com.hackathon.finance.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        LocalDate date,
        UUID accountId,
        String accountName,
        UUID destinationAccountId,
        String destinationAccountName,
        UUID categoryId,
        String categoryName,
        String merchant,
        String note,
        String paymentMethod,
        Set<String> tags,
        OffsetDateTime createdAt,
        UUID createdByUserId,
        String createdByDisplayName,
        List<String> alerts
) {
}
