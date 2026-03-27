package com.hackathon.finance.dto.report;

import java.util.List;

public record TrendsReportResponse(
        List<IncomeExpenseTrendItem> incomeExpenseTrend,
        List<SavingsRateTrendPointResponse> savingsRateTrend,
        List<CategoryTrendPointResponse> categoryTrends
) {
}
