package com.hackathon.finance.dto.forecast;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CashFlowForecastPeriodResponse(
        String label,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal openingBalance,
        BigDecimal projectedIncome,
        BigDecimal projectedExpense,
        BigDecimal netCashFlow,
        BigDecimal closingBalance,
        int recurringItems
) {
}
