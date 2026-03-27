package com.hackathon.finance.dto.forecast;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyForecastResponse(
        BigDecimal forecastedBalance,
        BigDecimal safeToSpend,
        List<String> warnings,
        List<CashFlowForecastPeriodResponse> periods
) {
}
