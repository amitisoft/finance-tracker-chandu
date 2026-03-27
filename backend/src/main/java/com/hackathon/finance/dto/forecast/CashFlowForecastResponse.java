package com.hackathon.finance.dto.forecast;

import java.math.BigDecimal;
import java.util.List;

public record CashFlowForecastResponse(
        int forecastMonths,
        BigDecimal currentBalance,
        BigDecimal projectedClosingBalance,
        BigDecimal projectedNetChange,
        BigDecimal averageMonthlyIncome,
        BigDecimal averageMonthlyExpense,
        BigDecimal lowestProjectedBalance,
        String healthSignal,
        List<CashFlowForecastPeriodResponse> periods
) {
}
