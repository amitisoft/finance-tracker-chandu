package com.hackathon.finance.dto.recurring;

import com.hackathon.finance.entity.enums.RecurringFrequency;
import com.hackathon.finance.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringResponse(
        UUID id,
        String title,
        TransactionType type,
        BigDecimal amount,
        UUID categoryId,
        String categoryName,
        UUID accountId,
        String accountName,
        RecurringFrequency frequency,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate nextRunDate,
        boolean autoCreateTransaction,
        boolean paused
) {
}
