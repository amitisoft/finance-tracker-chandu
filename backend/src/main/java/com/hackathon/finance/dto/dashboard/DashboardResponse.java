package com.hackathon.finance.dto.dashboard;

import com.hackathon.finance.dto.goal.GoalResponse;
import com.hackathon.finance.dto.recurring.RecurringResponse;
import com.hackathon.finance.dto.report.CategorySpendItem;
import com.hackathon.finance.dto.report.IncomeExpenseTrendItem;
import com.hackathon.finance.dto.transaction.TransactionResponse;
import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal currentMonthIncome,
        BigDecimal currentMonthExpense,
        BigDecimal netBalance,
        List<CategorySpendItem> spendingByCategory,
        List<IncomeExpenseTrendItem> incomeVsExpenseTrend,
        List<TransactionResponse> recentTransactions,
        List<RecurringResponse> upcomingRecurringPayments,
        List<GoalResponse> goals
) {
}
