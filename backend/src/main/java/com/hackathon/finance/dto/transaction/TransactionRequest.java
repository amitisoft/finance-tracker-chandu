package com.hackathon.finance.dto.transaction;

import com.hackathon.finance.entity.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record TransactionRequest(
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDate date,
        @NotNull UUID accountId,
        UUID destinationAccountId,
        UUID categoryId,
        @Size(max = 200) String merchant,
        @Size(max = 2000) String note,
        @Size(max = 50) String paymentMethod,
        Set<@Size(max = 60) String> tags,
        UUID recurringTransactionId
) {
}
