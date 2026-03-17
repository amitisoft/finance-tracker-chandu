package com.hackathon.finance.dto.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
        LocalDate targetDate,
        UUID linkedAccountId,
        @Size(max = 50) String icon,
        @Size(max = 20) String color
) {
}
