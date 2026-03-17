package com.hackathon.finance.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record BudgetRequest(
        @NotNull UUID categoryId,
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull @Min(1) @Max(120) Integer alertThresholdPercent
) {
}
