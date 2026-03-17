package com.hackathon.finance.dto.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record GoalContributionRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        UUID sourceAccountId
) {
}
