package com.hackathon.finance.dto.recurring;

import com.hackathon.finance.entity.enums.RecurringFrequency;
import com.hackathon.finance.entity.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecurringRequest(
        @NotBlank String title,
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        UUID categoryId,
        @NotNull UUID accountId,
        @NotNull RecurringFrequency frequency,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull Boolean autoCreateTransaction,
        @NotNull Boolean paused
) {
}
