package com.hackathon.finance.service;

import com.hackathon.finance.dto.dashboard.DashboardResponse;
import com.hackathon.finance.dto.goal.GoalResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionService transactionService;
    private final ReportService reportService;
    private final GoalService goalService;
    private final RecurringService recurringService;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
        List<com.hackathon.finance.dto.transaction.TransactionResponse> monthTransactions = transactionService.search(start, end, null, null, null, null);
        BigDecimal income = monthTransactions.stream()
                .filter(transaction -> transaction.type() == com.hackathon.finance.entity.enums.TransactionType.INCOME)
                .map(com.hackathon.finance.dto.transaction.TransactionResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expense = monthTransactions.stream()
                .filter(transaction -> transaction.type() == com.hackathon.finance.entity.enums.TransactionType.EXPENSE)
                .map(com.hackathon.finance.dto.transaction.TransactionResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netBalance = accountService.getAccounts().stream()
                .map(com.hackathon.finance.dto.account.AccountResponse::currentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<GoalResponse> goals = goalService.getGoals().stream().limit(4).toList();
        return new DashboardResponse(
                income,
                expense,
                netBalance,
                reportService.getCategorySpend(start, end),
                reportService.getIncomeVsExpense(today.minusMonths(5).withDayOfMonth(1), end),
                transactionService.recent(),
                recurringService.getAll().stream().filter(recurring -> !recurring.paused()).limit(5).toList(),
                goals
        );
    }
}
