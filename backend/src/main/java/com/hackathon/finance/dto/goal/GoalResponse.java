package com.hackathon.finance.dto.goal;

import com.hackathon.finance.entity.enums.GoalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        double progressPercentage,
        LocalDate targetDate,
        UUID linkedAccountId,
        String icon,
        String color,
        GoalStatus status
) {
}
