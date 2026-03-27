package com.hackathon.finance.service;

import com.hackathon.finance.dto.forecast.CashFlowForecastPeriodResponse;
import com.hackathon.finance.dto.forecast.CashFlowForecastResponse;
import com.hackathon.finance.dto.forecast.DailyForecastPointResponse;
import com.hackathon.finance.dto.forecast.MonthlyForecastResponse;
import com.hackathon.finance.entity.RecurringTransactionEntity;
import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.enums.RecurringFrequency;
import com.hackathon.finance.entity.enums.TransactionType;
import com.hackathon.finance.exception.BadRequestException;
import com.hackathon.finance.repository.RecurringTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CashFlowForecastService {

    private static final int DEFAULT_LOOKBACK_MONTHS = 3;
    private static final int MAX_FORECAST_MONTHS = 12;
    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final UserContextService userContextService;

    @Transactional(readOnly = true)
    public CashFlowForecastResponse getForecast(int months) {
        if (months < 1 || months > MAX_FORECAST_MONTHS) {
            throw new BadRequestException("Forecast months must be between 1 and " + MAX_FORECAST_MONTHS + ".");
        }

        BigDecimal currentBalance = normalize(accountService.getAccounts().stream()
                .map(account -> account.currentBalance() == null ? BigDecimal.ZERO : account.currentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        HistoricalAverages historicalAverages = calculateHistoricalAverages();
        List<RecurringTransactionEntity> activeRecurring = recurringTransactionRepository
                .findAllByUserAndPausedFalseOrderByNextRunDateAsc(userContextService.getCurrentUser());

        BigDecimal openingBalance = currentBalance;
        BigDecimal lowestBalance = currentBalance;
        List<CashFlowForecastPeriodResponse> periods = new ArrayList<>();
        YearMonth period = YearMonth.now();

        for (int index = 0; index < months; index++) {
            ForecastProjection recurringProjection = projectRecurringForMonth(activeRecurring, period);
            BigDecimal income = normalize(historicalAverages.averageIncome().add(recurringProjection.income()));
            BigDecimal expense = normalize(historicalAverages.averageExpense().add(recurringProjection.expense()));
            BigDecimal netCashFlow = normalize(income.subtract(expense));
            BigDecimal closingBalance = normalize(openingBalance.add(netCashFlow));
            lowestBalance = lowestBalance.min(closingBalance);

            periods.add(new CashFlowForecastPeriodResponse(
                    period.format(PERIOD_FORMATTER),
                    period.atDay(1),
                    period.atEndOfMonth(),
                    openingBalance,
                    income,
                    expense,
                    netCashFlow,
                    closingBalance,
                    recurringProjection.recurringItems()
            ));

            openingBalance = closingBalance;
            period = period.plusMonths(1);
        }

        BigDecimal projectedClosingBalance = periods.isEmpty()
                ? currentBalance
                : periods.get(periods.size() - 1).closingBalance();
        BigDecimal projectedNetChange = normalize(projectedClosingBalance.subtract(currentBalance));

        return new CashFlowForecastResponse(
                months,
                currentBalance,
                projectedClosingBalance,
                projectedNetChange,
                historicalAverages.averageIncome(),
                historicalAverages.averageExpense(),
                normalize(lowestBalance),
                healthSignal(lowestBalance, projectedNetChange),
                periods
        );
    }

    @Transactional(readOnly = true)
    public MonthlyForecastResponse getMonthlyForecast() {
        CashFlowForecastResponse forecast = getForecast(1);
        CashFlowForecastPeriodResponse period = forecast.periods().isEmpty()
                ? new CashFlowForecastPeriodResponse(
                YearMonth.now().format(PERIOD_FORMATTER),
                YearMonth.now().atDay(1),
                YearMonth.now().atEndOfMonth(),
                forecast.currentBalance(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                forecast.currentBalance(),
                0
        )
                : forecast.periods().get(0);
        BigDecimal monthlyExpenseBuffer = forecast.averageMonthlyExpense().multiply(BigDecimal.valueOf(0.25));
        BigDecimal safeToSpend = period.closingBalance().subtract(monthlyExpenseBuffer).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        List<String> warnings = new ArrayList<>();
        if (period.closingBalance().compareTo(BigDecimal.ZERO) < 0) {
            warnings.add("Projected month-end balance is negative.");
        }
        if (safeToSpend.compareTo(BigDecimal.ZERO) == 0) {
            warnings.add("Safe-to-spend is fully consumed after keeping a minimum monthly buffer.");
        }
        if (period.projectedExpense().compareTo(period.projectedIncome()) > 0) {
            warnings.add("Expected expenses are running ahead of projected income this month.");
        }
        return new MonthlyForecastResponse(period.closingBalance(), safeToSpend, warnings, forecast.periods());
    }

    @Transactional(readOnly = true)
    public List<DailyForecastPointResponse> getDailyForecast() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate start = LocalDate.now();
        LocalDate end = currentMonth.atEndOfMonth();
        BigDecimal openingBalance = normalize(accountService.getAccounts().stream()
                .map(account -> account.currentBalance() == null ? BigDecimal.ZERO : account.currentBalance())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        HistoricalAverages historicalAverages = calculateHistoricalAverages();
        List<RecurringTransactionEntity> recurringItems = recurringTransactionRepository
                .findAllByUserAndPausedFalseOrderByNextRunDateAsc(userContextService.getCurrentUser());

        BigDecimal estimatedDailyIncome = historicalAverages.averageIncome()
                .divide(BigDecimal.valueOf(Math.max(currentMonth.lengthOfMonth(), 1L)), 2, RoundingMode.HALF_UP);
        BigDecimal estimatedDailyExpense = historicalAverages.averageExpense()
                .divide(BigDecimal.valueOf(Math.max(currentMonth.lengthOfMonth(), 1L)), 2, RoundingMode.HALF_UP);

        List<DailyForecastPointResponse> points = new ArrayList<>();
        BigDecimal rollingBalance = openingBalance;
        for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(1)) {
            BigDecimal knownExpense = BigDecimal.ZERO;
            BigDecimal knownIncome = BigDecimal.ZERO;
            for (RecurringTransactionEntity recurring : recurringItems) {
                if (!occursOn(recurring, cursor)) {
                    continue;
                }
                if (recurring.getType() == TransactionType.EXPENSE) {
                    knownExpense = knownExpense.add(recurring.getAmount());
                } else if (recurring.getType() == TransactionType.INCOME) {
                    knownIncome = knownIncome.add(recurring.getAmount());
                }
            }
            rollingBalance = rollingBalance
                    .add(estimatedDailyIncome)
                    .subtract(estimatedDailyExpense)
                    .add(knownIncome)
                    .subtract(knownExpense);
            points.add(new DailyForecastPointResponse(cursor, normalize(rollingBalance), normalize(knownExpense)));
        }
        return points;
    }

    private HistoricalAverages calculateHistoricalAverages() {
        LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate fromDate = currentMonthStart.minusMonths(DEFAULT_LOOKBACK_MONTHS);
        LocalDate toDate = currentMonthStart.minusDays(1);
        List<TransactionEntity> transactions = transactionService.findTransactionsForRange(fromDate, toDate);

        BigDecimal totalIncome = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.INCOME)
                .filter(transaction -> transaction.getRecurringTransaction() == null)
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .filter(transaction -> transaction.getRecurringTransaction() == null)
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new HistoricalAverages(
                normalize(totalIncome.divide(BigDecimal.valueOf(DEFAULT_LOOKBACK_MONTHS), 2, RoundingMode.HALF_UP)),
                normalize(totalExpense.divide(BigDecimal.valueOf(DEFAULT_LOOKBACK_MONTHS), 2, RoundingMode.HALF_UP))
        );
    }

    private ForecastProjection projectRecurringForMonth(List<RecurringTransactionEntity> recurringItems, YearMonth month) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int recurringCount = 0;

        for (RecurringTransactionEntity recurring : recurringItems) {
            int occurrences = countOccurrences(recurring, month);
            if (occurrences == 0) {
                continue;
            }
            recurringCount += occurrences;
            BigDecimal amount = recurring.getAmount().multiply(BigDecimal.valueOf(occurrences));
            if (recurring.getType() == TransactionType.INCOME) {
                income = income.add(amount);
            } else if (recurring.getType() == TransactionType.EXPENSE) {
                expense = expense.add(amount);
            }
        }

        return new ForecastProjection(normalize(income), normalize(expense), recurringCount);
    }

    private int countOccurrences(RecurringTransactionEntity recurring, YearMonth month) {
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();
        LocalDate cursor = recurring.getNextRunDate().isBefore(recurring.getStartDate())
                ? recurring.getStartDate()
                : recurring.getNextRunDate();
        int count = 0;

        while (cursor.isBefore(periodStart)) {
            cursor = nextRunDate(cursor, recurring.getFrequency());
        }

        while (!cursor.isAfter(periodEnd)) {
            if (recurring.getEndDate() != null && cursor.isAfter(recurring.getEndDate())) {
                break;
            }
            count++;
            cursor = nextRunDate(cursor, recurring.getFrequency());
        }

        return count;
    }

    private boolean occursOn(RecurringTransactionEntity recurring, LocalDate date) {
        if (date.isBefore(recurring.getStartDate())) {
            return false;
        }
        if (recurring.getEndDate() != null && date.isAfter(recurring.getEndDate())) {
            return false;
        }
        LocalDate cursor = recurring.getNextRunDate().isBefore(recurring.getStartDate())
                ? recurring.getStartDate()
                : recurring.getNextRunDate();
        while (cursor.isBefore(date)) {
            cursor = nextRunDate(cursor, recurring.getFrequency());
        }
        return cursor.equals(date);
    }

    private LocalDate nextRunDate(LocalDate current, RecurringFrequency frequency) {
        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case YEARLY -> current.plusYears(1);
        };
    }

    private String healthSignal(BigDecimal lowestBalance, BigDecimal projectedNetChange) {
        if (lowestBalance.compareTo(BigDecimal.ZERO) < 0) {
            return "CRITICAL";
        }
        if (projectedNetChange.compareTo(BigDecimal.ZERO) < 0) {
            return "WATCH";
        }
        return "STABLE";
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record HistoricalAverages(BigDecimal averageIncome, BigDecimal averageExpense) {
    }

    private record ForecastProjection(BigDecimal income, BigDecimal expense, int recurringItems) {
    }
}
