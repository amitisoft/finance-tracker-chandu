package com.hackathon.finance.dto.forecast;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyForecastPointResponse(
        LocalDate date,
        BigDecimal projectedBalance,
        BigDecimal knownExpenses
) {
}
