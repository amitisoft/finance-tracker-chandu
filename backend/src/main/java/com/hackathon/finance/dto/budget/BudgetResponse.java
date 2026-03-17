package com.hackathon.finance.dto.budget;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        BigDecimal actualSpent,
        BigDecimal remaining,
        double percentageUsed,
        int month,
        int year,
        int alertThresholdPercent
) {
}
