package com.hackathon.finance.service;

import com.hackathon.finance.dto.report.AccountBalanceTrendItem;
import com.hackathon.finance.dto.report.CategorySpendItem;
import com.hackathon.finance.dto.report.IncomeExpenseTrendItem;
import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TransactionService transactionService;
    private final AccountService accountService;

    @Transactional(readOnly = true)
    public List<CategorySpendItem> getCategorySpend(LocalDate fromDate, LocalDate toDate) {
        return transactionService.findTransactionsForRange(fromDate, toDate).stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE && transaction.getCategory() != null)
                .collect(Collectors.groupingBy(transaction -> transaction.getCategory().getName(),
                        Collectors.mapping(TransactionEntity::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream()
                .map(entry -> new CategorySpendItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategorySpendItem::total).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IncomeExpenseTrendItem> getIncomeVsExpense(LocalDate fromDate, LocalDate toDate) {
        Map<YearMonth, List<TransactionEntity>> grouped = transactionService.findTransactionsForRange(fromDate, toDate).stream()
                .collect(Collectors.groupingBy(transaction -> YearMonth.from(transaction.getTransactionDate())));
        List<IncomeExpenseTrendItem> items = new ArrayList<>();
        YearMonth current = YearMonth.from(fromDate);
        YearMonth end = YearMonth.from(toDate);
        while (!current.isAfter(end)) {
            List<TransactionEntity> transactions = grouped.getOrDefault(current, List.of());
            BigDecimal income = sumByType(transactions, TransactionType.INCOME);
            BigDecimal expense = sumByType(transactions, TransactionType.EXPENSE);
            items.add(new IncomeExpenseTrendItem(current.format(DateTimeFormatter.ofPattern("MMM yyyy")), income, expense));
            current = current.plusMonths(1);
        }
        return items;
    }

    @Transactional(readOnly = true)
    public List<AccountBalanceTrendItem> getAccountBalanceTrend() {
        return accountService.getAccounts().stream()
                .map(account -> new AccountBalanceTrendItem(account.name(), account.currentBalance()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<String> exportTransactionsCsv(LocalDate fromDate, LocalDate toDate) {
        StringBuilder builder = new StringBuilder("Date,Type,Account,Category,Merchant,Amount,Note\n");
        transactionService.search(fromDate, toDate, null, null, null, null).forEach(transaction ->
                builder.append(transaction.date()).append(',')
                        .append(transaction.type()).append(',')
                        .append(escape(transaction.accountName())).append(',')
                        .append(escape(transaction.categoryName())).append(',')
                        .append(escape(transaction.merchant())).append(',')
                        .append(transaction.amount()).append(',')
                        .append(escape(transaction.note())).append('\n')
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(builder.toString());
    }

    private BigDecimal sumByType(List<TransactionEntity> transactions, TransactionType type) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
