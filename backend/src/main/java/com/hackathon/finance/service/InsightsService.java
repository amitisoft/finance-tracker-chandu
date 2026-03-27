package com.hackathon.finance.service;

import com.hackathon.finance.dto.insight.HealthScoreBreakdownItem;
import com.hackathon.finance.dto.insight.HealthScoreResponse;
import com.hackathon.finance.dto.insight.InsightItemResponse;
import com.hackathon.finance.dto.report.CategoryTrendPointResponse;
import com.hackathon.finance.dto.report.IncomeExpenseTrendItem;
import com.hackathon.finance.dto.report.NetWorthPointResponse;
import com.hackathon.finance.dto.report.SavingsRateTrendPointResponse;
import com.hackathon.finance.dto.report.TrendsReportResponse;
import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsightsService {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final BudgetService budgetService;

    @Transactional(readOnly = true)
    public HealthScoreResponse getHealthScore() {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusMonths(3).withDayOfMonth(1);
        List<TransactionEntity> transactions = transactionService.findTransactionsForRange(fromDate, today);

        BigDecimal totalIncome = sumByType(transactions, TransactionType.INCOME);
        BigDecimal totalExpense = sumByType(transactions, TransactionType.EXPENSE);
        BigDecimal monthlyExpenseAverage = divide(totalExpense, 3);
        BigDecimal monthlyIncomeAverage = divide(totalIncome, 3);
        BigDecimal totalBalance = accountService.getAccounts().stream()
                .map(account -> account.currentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double savingsRateScore = scoreSavingsRate(monthlyIncomeAverage, monthlyExpenseAverage);
        double stabilityScore = scoreExpenseStability(transactions);
        double budgetScore = scoreBudgetAdherence(today);
        double cashBufferScore = scoreCashBuffer(totalBalance, monthlyExpenseAverage);

        double finalScore = round(
                (savingsRateScore * 0.30)
                        + (stabilityScore * 0.20)
                        + (budgetScore * 0.20)
                        + (cashBufferScore * 0.30)
        );

        List<HealthScoreBreakdownItem> breakdown = List.of(
                new HealthScoreBreakdownItem("Savings rate", savingsRateScore, "Income retained after expenses"),
                new HealthScoreBreakdownItem("Expense stability", stabilityScore, "How predictable recent spending has been"),
                new HealthScoreBreakdownItem("Budget adherence", budgetScore, "How often spend stays near budget"),
                new HealthScoreBreakdownItem("Cash buffer", cashBufferScore, "Balance coverage relative to monthly expenses")
        );

        return new HealthScoreResponse(finalScore, scoreBand(finalScore), breakdown, buildSuggestions(finalScore, savingsRateScore, budgetScore, cashBufferScore));
    }

    @Transactional(readOnly = true)
    public List<InsightItemResponse> getInsights() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        List<TransactionEntity> transactions = transactionService.findTransactionsForRange(previousMonth.atDay(1), currentMonth.atEndOfMonth());

        BigDecimal currentFood = categoryTotal(transactions, currentMonth, "Food");
        BigDecimal previousFood = categoryTotal(transactions, previousMonth, "Food");
        BigDecimal currentIncome = monthTotalByType(transactions, currentMonth, TransactionType.INCOME);
        BigDecimal currentExpense = monthTotalByType(transactions, currentMonth, TransactionType.EXPENSE);
        BigDecimal previousExpense = monthTotalByType(transactions, previousMonth, TransactionType.EXPENSE);

        List<InsightItemResponse> insights = new ArrayList<>();
        if (previousFood.signum() > 0) {
            double change = currentFood.subtract(previousFood)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousFood, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            insights.add(new InsightItemResponse("Food spending trend",
                    "Your food spending changed by " + change + "% compared with last month.",
                    change > 0 ? "warning" : "success"));
        }
        insights.add(new InsightItemResponse("Monthly savings snapshot",
                "This month you kept " + formatPercent(scoreSavingsRate(currentIncome, currentExpense)) + " of income after expenses.",
                currentIncome.compareTo(currentExpense) >= 0 ? "success" : "warning"));
        insights.add(new InsightItemResponse("Expense movement",
                "Total expenses changed by " + expenseChange(previousExpense, currentExpense) + " versus last month.",
                currentExpense.compareTo(previousExpense) <= 0 ? "success" : "warning"));
        return insights;
    }

    @Transactional(readOnly = true)
    public TrendsReportResponse getTrends(LocalDate fromDate, LocalDate toDate) {
        List<IncomeExpenseTrendItem> incomeExpenseTrend = buildIncomeExpenseTrend(fromDate, toDate);
        List<SavingsRateTrendPointResponse> savingsRateTrend = incomeExpenseTrend.stream()
                .map(item -> new SavingsRateTrendPointResponse(item.month(),
                        item.income().signum() == 0 ? 0 : round(item.income().subtract(item.expense())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(item.income(), 2, RoundingMode.HALF_UP)
                                .doubleValue())))
                .toList();
        List<CategoryTrendPointResponse> categoryTrends = buildCategoryTrends(fromDate, toDate);
        return new TrendsReportResponse(incomeExpenseTrend, savingsRateTrend, categoryTrends);
    }

    @Transactional(readOnly = true)
    public List<NetWorthPointResponse> getNetWorthTrend(LocalDate fromDate, LocalDate toDate) {
        List<IncomeExpenseTrendItem> incomeExpenseTrend = buildIncomeExpenseTrend(fromDate, toDate);
        BigDecimal currentNetWorth = accountService.getAccounts().stream()
                .map(account -> account.currentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<NetWorthPointResponse> points = new ArrayList<>();
        BigDecimal rolling = currentNetWorth.subtract(incomeExpenseTrend.stream()
                .map(item -> item.income().subtract(item.expense()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        for (IncomeExpenseTrendItem item : incomeExpenseTrend) {
            rolling = rolling.add(item.income().subtract(item.expense()));
            points.add(new NetWorthPointResponse(item.month(), rolling.setScale(2, RoundingMode.HALF_UP)));
        }
        return points;
    }

    private List<IncomeExpenseTrendItem> buildIncomeExpenseTrend(LocalDate fromDate, LocalDate toDate) {
        Map<YearMonth, List<TransactionEntity>> grouped = transactionService.findTransactionsForRange(fromDate, toDate).stream()
                .collect(Collectors.groupingBy(transaction -> YearMonth.from(transaction.getTransactionDate())));
        List<IncomeExpenseTrendItem> items = new ArrayList<>();
        YearMonth current = YearMonth.from(fromDate);
        YearMonth end = YearMonth.from(toDate);
        while (!current.isAfter(end)) {
            List<TransactionEntity> monthTransactions = grouped.getOrDefault(current, List.of());
            items.add(new IncomeExpenseTrendItem(
                    current.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                    sumByType(monthTransactions, TransactionType.INCOME),
                    sumByType(monthTransactions, TransactionType.EXPENSE)
            ));
            current = current.plusMonths(1);
        }
        return items;
    }

    private List<CategoryTrendPointResponse> buildCategoryTrends(LocalDate fromDate, LocalDate toDate) {
        Map<String, BigDecimal> byMonthAndCategory = new LinkedHashMap<>();
        transactionService.findTransactionsForRange(fromDate, toDate).stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE && transaction.getCategory() != null)
                .forEach(transaction -> {
                    String key = YearMonth.from(transaction.getTransactionDate()).format(DateTimeFormatter.ofPattern("MMM yyyy"))
                            + "|" + transaction.getCategory().getName();
                    byMonthAndCategory.merge(key, transaction.getAmount(), BigDecimal::add);
                });
        return byMonthAndCategory.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    return new CategoryTrendPointResponse(parts[0], parts[1], entry.getValue());
                })
                .sorted(Comparator.comparing(CategoryTrendPointResponse::month))
                .toList();
    }

    private double scoreSavingsRate(BigDecimal monthlyIncome, BigDecimal monthlyExpense) {
        if (monthlyIncome.signum() <= 0) {
            return 25;
        }
        double rate = monthlyIncome.subtract(monthlyExpense)
                .multiply(BigDecimal.valueOf(100))
                .divide(monthlyIncome, 2, RoundingMode.HALF_UP)
                .doubleValue();
        return Math.max(0, Math.min(100, rate + 50));
    }

    private double scoreExpenseStability(List<TransactionEntity> transactions) {
        Map<YearMonth, BigDecimal> monthlyExpenses = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(transaction -> YearMonth.from(transaction.getTransactionDate()),
                        Collectors.mapping(TransactionEntity::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        if (monthlyExpenses.size() <= 1) {
            return 70;
        }
        double avg = monthlyExpenses.values().stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        if (avg == 0) {
            return 80;
        }
        double variance = monthlyExpenses.values().stream()
                .mapToDouble(value -> Math.pow(value.doubleValue() - avg, 2))
                .average()
                .orElse(0);
        double coefficient = Math.sqrt(variance) / avg;
        return round(Math.max(0, 100 - (coefficient * 100)));
    }

    private double scoreBudgetAdherence(LocalDate today) {
        List<com.hackathon.finance.dto.budget.BudgetResponse> budgets = budgetService.getBudgets(today.getMonthValue(), today.getYear());
        if (budgets.isEmpty()) {
            return 60;
        }
        return round(budgets.stream()
                .mapToDouble(budget -> Math.max(0, 100 - Math.abs(100 - budget.percentageUsed())))
                .average()
                .orElse(60));
    }

    private double scoreCashBuffer(BigDecimal totalBalance, BigDecimal monthlyExpenseAverage) {
        if (monthlyExpenseAverage.signum() <= 0) {
            return 90;
        }
        double monthsCovered = totalBalance.divide(monthlyExpenseAverage, 2, RoundingMode.HALF_UP).doubleValue();
        return round(Math.max(0, Math.min(100, monthsCovered * 25)));
    }

    private List<String> buildSuggestions(double finalScore, double savings, double budget, double cashBuffer) {
        List<String> suggestions = new ArrayList<>();
        if (savings < 60) {
            suggestions.add("Reduce non-essential expenses or grow income to improve monthly savings rate.");
        }
        if (budget < 65) {
            suggestions.add("Review budget categories that are drifting away from plan.");
        }
        if (cashBuffer < 60) {
            suggestions.add("Build a larger cash buffer to cover at least 2-4 months of expenses.");
        }
        if (finalScore >= 80) {
            suggestions.add("Your overall financial health is strong. Maintain consistency and continue tracking trends.");
        }
        return suggestions;
    }

    private BigDecimal sumByType(List<TransactionEntity> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal divide(BigDecimal amount, int divisor) {
        return amount.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal categoryTotal(List<TransactionEntity> transactions, YearMonth month, String categoryName) {
        return transactions.stream()
                .filter(transaction -> YearMonth.from(transaction.getTransactionDate()).equals(month))
                .filter(transaction -> transaction.getCategory() != null && categoryName.equalsIgnoreCase(transaction.getCategory().getName()))
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal monthTotalByType(List<TransactionEntity> transactions, YearMonth month, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> YearMonth.from(transaction.getTransactionDate()).equals(month))
                .filter(transaction -> transaction.getType() == type)
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String expenseChange(BigDecimal previous, BigDecimal current) {
        if (previous.signum() == 0) {
            return "0%";
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }

    private String formatPercent(double value) {
        return round(value) + "%";
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String scoreBand(double score) {
        if (score >= 80) {
            return "STRONG";
        }
        if (score >= 60) {
            return "STABLE";
        }
        if (score >= 40) {
            return "WATCH";
        }
        return "AT_RISK";
    }
}
