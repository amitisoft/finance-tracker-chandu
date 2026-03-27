package com.hackathon.finance.config;

import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.AccountMemberEntity;
import com.hackathon.finance.entity.BudgetEntity;
import com.hackathon.finance.entity.CategoryEntity;
import com.hackathon.finance.entity.GoalEntity;
import com.hackathon.finance.entity.RecurringTransactionEntity;
import com.hackathon.finance.entity.RuleEntity;
import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.entity.enums.AccountMemberRole;
import com.hackathon.finance.entity.enums.AccountType;
import com.hackathon.finance.entity.enums.CategoryType;
import com.hackathon.finance.entity.enums.GoalStatus;
import com.hackathon.finance.entity.enums.RecurringFrequency;
import com.hackathon.finance.entity.enums.RuleActionType;
import com.hackathon.finance.entity.enums.RuleConditionField;
import com.hackathon.finance.entity.enums.RuleOperator;
import com.hackathon.finance.entity.enums.TransactionType;
import com.hackathon.finance.repository.AccountMemberRepository;
import com.hackathon.finance.repository.AccountRepository;
import com.hackathon.finance.repository.BudgetRepository;
import com.hackathon.finance.repository.CategoryRepository;
import com.hackathon.finance.repository.GoalRepository;
import com.hackathon.finance.repository.RecurringTransactionRepository;
import com.hackathon.finance.repository.RuleRepository;
import com.hackathon.finance.repository.TransactionRepository;
import com.hackathon.finance.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    public static final String DEMO_EMAIL = "demo@financetracker.local";
    public static final String DEMO_PASSWORD = "Demo@12345";
    public static final String COLLABORATOR_EMAIL = "partner@financetracker.local";
    public static final String COLLABORATOR_PASSWORD = "Partner@12345";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final AccountMemberRepository accountMemberRepository;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
    private final RuleRepository ruleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase(DEMO_EMAIL)) {
            return;
        }

        UserEntity demoUser = createUser(DEMO_EMAIL, "Demo User", DEMO_PASSWORD);
        UserEntity collaborator = createUser(COLLABORATOR_EMAIL, "Avery Partner", COLLABORATOR_PASSWORD);

        Map<String, CategoryEntity> categories = seedCategories(demoUser);
        seedCategories(collaborator);

        AccountEntity checking = createAccount(demoUser, "Daily Spend", AccountType.BANK_ACCOUNT, "4300.00", "Atlas Bank");
        AccountEntity savings = createAccount(demoUser, "Rainy Day Savings", AccountType.SAVINGS_ACCOUNT, "15000.00", "Atlas Bank");
        AccountEntity cashWallet = createAccount(demoUser, "Weekend Wallet", AccountType.CASH_WALLET, "700.00", "Personal");
        AccountEntity sharedHome = createAccount(demoUser, "Shared Home Account", AccountType.BANK_ACCOUNT, "3800.00", "Atlas Bank");

        AccountMemberEntity membership = new AccountMemberEntity();
        membership.setAccount(sharedHome);
        membership.setUser(collaborator);
        membership.setInvitedBy(demoUser);
        membership.setRole(AccountMemberRole.EDITOR);
        accountMemberRepository.save(membership);

        Map<String, RecurringTransactionEntity> recurring = seedRecurring(demoUser, checking, sharedHome, categories);
        seedRules(demoUser);
        seedHistoricalTransactions(demoUser, collaborator, checking, savings, cashWallet, sharedHome, categories, recurring);
        seedBudgets(demoUser, categories);
        seedGoals(demoUser, savings, sharedHome);

        accountRepository.saveAll(List.of(checking, savings, cashWallet, sharedHome));
    }

    private UserEntity createUser(String email, String displayName, String password) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private Map<String, CategoryEntity> seedCategories(UserEntity user) {
        Map<String, CategoryEntity> categories = new LinkedHashMap<>();
        categories.put("Food", saveCategory(user, "Food", CategoryType.EXPENSE, "#ef4444", "utensils"));
        categories.put("Rent", saveCategory(user, "Rent", CategoryType.EXPENSE, "#f97316", "home"));
        categories.put("Utilities", saveCategory(user, "Utilities", CategoryType.EXPENSE, "#f59e0b", "bolt"));
        categories.put("Transport", saveCategory(user, "Transport", CategoryType.EXPENSE, "#3b82f6", "car"));
        categories.put("Entertainment", saveCategory(user, "Entertainment", CategoryType.EXPENSE, "#8b5cf6", "film"));
        categories.put("Shopping", saveCategory(user, "Shopping", CategoryType.EXPENSE, "#ec4899", "bag"));
        categories.put("Health", saveCategory(user, "Health", CategoryType.EXPENSE, "#22c55e", "heart"));
        categories.put("Travel", saveCategory(user, "Travel", CategoryType.EXPENSE, "#06b6d4", "plane"));
        categories.put("Subscriptions", saveCategory(user, "Subscriptions", CategoryType.EXPENSE, "#6366f1", "tv"));
        categories.put("Education", saveCategory(user, "Education", CategoryType.EXPENSE, "#14b8a6", "book"));
        categories.put("Salary", saveCategory(user, "Salary", CategoryType.INCOME, "#10b981", "briefcase"));
        categories.put("Freelance", saveCategory(user, "Freelance", CategoryType.INCOME, "#0ea5e9", "laptop"));
        categories.put("Bonus", saveCategory(user, "Bonus", CategoryType.INCOME, "#84cc16", "gift"));
        categories.put("Investment", saveCategory(user, "Investment", CategoryType.INCOME, "#22c55e", "chart"));
        return categories;
    }

    private CategoryEntity saveCategory(UserEntity user, String name, CategoryType type, String color, String icon) {
        CategoryEntity category = new CategoryEntity();
        category.setUser(user);
        category.setName(name);
        category.setType(type);
        category.setColor(color);
        category.setIcon(icon);
        return categoryRepository.save(category);
    }

    private AccountEntity createAccount(UserEntity user, String name, AccountType type, String openingBalance, String institutionName) {
        AccountEntity account = new AccountEntity();
        account.setUser(user);
        account.setName(name);
        account.setType(type);
        account.setOpeningBalance(amount(openingBalance));
        account.setCurrentBalance(amount(openingBalance));
        account.setInstitutionName(institutionName);
        return accountRepository.save(account);
    }

    private Map<String, RecurringTransactionEntity> seedRecurring(
            UserEntity demoUser,
            AccountEntity checking,
            AccountEntity sharedHome,
            Map<String, CategoryEntity> categories
    ) {
        Map<String, RecurringTransactionEntity> recurring = new LinkedHashMap<>();
        recurring.put("salary", saveRecurring(demoUser, checking, categories.get("Salary"), "Main Salary", TransactionType.INCOME, "9250.00", 1));
        recurring.put("rent", saveRecurring(demoUser, sharedHome, categories.get("Rent"), "Apartment Rent", TransactionType.EXPENSE, "2350.00", 3));
        recurring.put("utilities", saveRecurring(demoUser, sharedHome, categories.get("Utilities"), "Utilities Bundle", TransactionType.EXPENSE, "340.00", 7));
        recurring.put("streaming", saveRecurring(demoUser, checking, categories.get("Subscriptions"), "Streaming Stack", TransactionType.EXPENSE, "48.00", 11));
        recurring.put("gym", saveRecurring(demoUser, checking, categories.get("Health"), "Gym Membership", TransactionType.EXPENSE, "65.00", 14));
        return recurring;
    }

    private RecurringTransactionEntity saveRecurring(
            UserEntity user,
            AccountEntity account,
            CategoryEntity category,
            String title,
            TransactionType type,
            String amount,
            int dayOfMonth
    ) {
        LocalDate nextRunDate = nextRunDate(dayOfMonth);
        RecurringTransactionEntity recurring = new RecurringTransactionEntity();
        recurring.setUser(user);
        recurring.setTitle(title);
        recurring.setType(type);
        recurring.setAmount(amount(amount));
        recurring.setCategory(category);
        recurring.setAccount(account);
        recurring.setFrequency(RecurringFrequency.MONTHLY);
        recurring.setStartDate(LocalDate.now().minusYears(2));
        recurring.setNextRunDate(nextRunDate);
        recurring.setAutoCreateTransaction(true);
        recurring.setPaused(false);
        return recurringTransactionRepository.save(recurring);
    }

    private void seedRules(UserEntity user) {
        RuleEntity uberRule = new RuleEntity();
        uberRule.setUser(user);
        uberRule.setConditionField(RuleConditionField.MERCHANT);
        uberRule.setConditionOperator(RuleOperator.CONTAINS);
        uberRule.setConditionValue("uber");
        uberRule.setActionType(RuleActionType.SET_CATEGORY);
        uberRule.setActionValue("Transport");
        uberRule.setActive(true);
        uberRule.setPriority(10);

        RuleEntity alertRule = new RuleEntity();
        alertRule.setUser(user);
        alertRule.setConditionField(RuleConditionField.AMOUNT);
        alertRule.setConditionOperator(RuleOperator.GREATER_THAN);
        alertRule.setConditionValue("5000");
        alertRule.setActionType(RuleActionType.TRIGGER_ALERT);
        alertRule.setActionValue("Large transaction detected");
        alertRule.setActive(true);
        alertRule.setPriority(20);

        RuleEntity foodRule = new RuleEntity();
        foodRule.setUser(user);
        foodRule.setConditionField(RuleConditionField.CATEGORY);
        foodRule.setConditionOperator(RuleOperator.EQUALS);
        foodRule.setConditionValue("Food");
        foodRule.setActionType(RuleActionType.ADD_TAG);
        foodRule.setActionValue("monthly-food");
        foodRule.setActive(true);
        foodRule.setPriority(30);

        ruleRepository.saveAll(List.of(uberRule, alertRule, foodRule));
    }

    private void seedHistoricalTransactions(
            UserEntity demoUser,
            UserEntity collaborator,
            AccountEntity checking,
            AccountEntity savings,
            AccountEntity cashWallet,
            AccountEntity sharedHome,
            Map<String, CategoryEntity> categories,
            Map<String, RecurringTransactionEntity> recurring
    ) {
        YearMonth currentMonth = YearMonth.now();
        for (int monthOffset = 23; monthOffset >= 0; monthOffset--) {
            YearMonth month = currentMonth.minusMonths(monthOffset);
            int index = 23 - monthOffset;
            boolean current = month.equals(currentMonth);
            boolean previous = month.equals(currentMonth.minusMonths(1));

            createTransaction(demoUser, checking, null, categories.get("Salary"), recurring.get("salary"), TransactionType.INCOME,
                    amount(9250 + (index * 55L)), month.atDay(1), "Northstar Labs Payroll", "Primary salary deposit", "BANK");

            if (index % 4 == 1) {
                createTransaction(demoUser, checking, null, categories.get("Freelance"), null, TransactionType.INCOME,
                        amount(1450 + (index * 20L)), month.atDay(19), "Freelance Client", "Product consulting payout", "BANK");
            }
            if (index % 6 == 3) {
                createTransaction(demoUser, checking, null, categories.get("Bonus"), null, TransactionType.INCOME,
                        amount(2300 + (index * 15L)), month.atDay(27), "Performance Bonus", "Quarterly bonus", "BANK");
            }

            createTransfer(demoUser, checking, savings, amount(1800 + ((index % 3) * 150L)), month.atDay(2), "Monthly savings sweep");
            createTransaction(demoUser, sharedHome, null, categories.get("Rent"), recurring.get("rent"), TransactionType.EXPENSE,
                    amount(2350), month.atDay(3), "Oak Residency", "Rent for " + month.getMonth(), "BANK");
            createTransaction(demoUser, sharedHome, null, categories.get("Utilities"), recurring.get("utilities"), TransactionType.EXPENSE,
                    amount(310 + ((index % 5) * 12L)), month.atDay(7), "City Utilities", "Electricity and water", "BANK");
            createTransaction(demoUser, checking, null, categories.get("Health"), recurring.get("gym"), TransactionType.EXPENSE,
                    amount(65), month.atDay(14), "Motion Gym", "Monthly membership", "CARD");
            createTransaction(demoUser, checking, null, categories.get("Subscriptions"), recurring.get("streaming"), TransactionType.EXPENSE,
                    amount(48), month.atDay(11), "StreamHub", "Streaming bundle", "CARD");

            BigDecimal groceryAmount = amount(current ? 1180 : previous ? 860 : 690 + ((index % 4) * 40L));
            BigDecimal diningAmount = amount(current ? 520 : previous ? 340 : 230 + ((index % 3) * 35L));
            createTransaction(collaborator, sharedHome, null, categories.get("Food"), null, TransactionType.EXPENSE,
                    groceryAmount, month.atDay(9), "Fresh Basket", "Weekly grocery restock", "CARD", "monthly-food", "family");
            createTransaction(demoUser, checking, null, categories.get("Food"), null, TransactionType.EXPENSE,
                    diningAmount, month.atDay(16), index % 5 == 0 ? "Uber Eats" : "Neighborhood Bistro", "Dining and quick meals", "CARD", "monthly-food");

            createTransaction(demoUser, checking, null, categories.get("Transport"), null, TransactionType.EXPENSE,
                    amount(190 + ((index % 4) * 22L)), month.atDay(12), index % 2 == 0 ? "Uber" : "Metro Fuel", "Commute and transit", "CARD");
            createTransaction(demoUser, cashWallet, null, categories.get("Entertainment"), null, TransactionType.EXPENSE,
                    amount(120 + ((index % 6) * 18L)), month.atDay(20), "Weekend Events", "Movies and local events", "CASH");
            createTransaction(demoUser, checking, null, categories.get("Shopping"), null, TransactionType.EXPENSE,
                    amount(260 + ((index % 5) * 45L)), month.atDay(22), "Market Street", "Household shopping", "CARD");

            if (index % 3 == 0) {
                createTransaction(demoUser, checking, null, categories.get("Travel"), null, TransactionType.EXPENSE,
                        amount(540 + ((index % 4) * 90L)), month.atDay(24), "Regional Getaway", "Weekend travel booking", "CARD");
            }
            if (index % 4 == 2) {
                createTransaction(demoUser, checking, null, categories.get("Health"), null, TransactionType.EXPENSE,
                        amount(180 + ((index % 3) * 25L)), month.atDay(26), "Care Clinic", "Checkup and pharmacy", "CARD");
            }
            if (current) {
                createTransaction(collaborator, sharedHome, null, categories.get("Utilities"), null, TransactionType.EXPENSE,
                        amount(145), month.atDay(Math.min(LocalDate.now().getDayOfMonth(), month.lengthOfMonth())), "Quick Repairs", "Small home fix", "CARD");
            }
        }
    }

    private void seedBudgets(UserEntity user, Map<String, CategoryEntity> categories) {
        YearMonth current = YearMonth.now();
        budgetRepository.save(createBudget(user, categories.get("Food"), current, "1800", 80));
        budgetRepository.save(createBudget(user, categories.get("Utilities"), current, "500", 85));
        budgetRepository.save(createBudget(user, categories.get("Transport"), current, "420", 75));
        budgetRepository.save(createBudget(user, categories.get("Entertainment"), current, "350", 80));
        budgetRepository.save(createBudget(user, categories.get("Shopping"), current, "650", 85));
        budgetRepository.save(createBudget(user, categories.get("Health"), current, "300", 90));
    }

    private BudgetEntity createBudget(UserEntity user, CategoryEntity category, YearMonth month, String amount, int threshold) {
        BudgetEntity budget = new BudgetEntity();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(month.getMonthValue());
        budget.setYear(month.getYear());
        budget.setAmount(amount(amount));
        budget.setAlertThresholdPercent(threshold);
        return budget;
    }

    private void seedGoals(UserEntity user, AccountEntity savings, AccountEntity sharedHome) {
        GoalEntity emergencyFund = new GoalEntity();
        emergencyFund.setUser(user);
        emergencyFund.setLinkedAccount(savings);
        emergencyFund.setName("Emergency Fund");
        emergencyFund.setTargetAmount(amount("100000"));
        emergencyFund.setCurrentAmount(amount("74250"));
        emergencyFund.setTargetDate(LocalDate.now().plusMonths(14));
        emergencyFund.setIcon("shield");
        emergencyFund.setColor("#0f766e");
        emergencyFund.setStatus(GoalStatus.ACTIVE);

        GoalEntity vacation = new GoalEntity();
        vacation.setUser(user);
        vacation.setLinkedAccount(savings);
        vacation.setName("Japan Vacation");
        vacation.setTargetAmount(amount("25000"));
        vacation.setCurrentAmount(amount("14800"));
        vacation.setTargetDate(LocalDate.now().plusMonths(8));
        vacation.setIcon("plane");
        vacation.setColor("#f97316");
        vacation.setStatus(GoalStatus.ACTIVE);

        GoalEntity homeRefresh = new GoalEntity();
        homeRefresh.setUser(user);
        homeRefresh.setLinkedAccount(sharedHome);
        homeRefresh.setName("Living Room Refresh");
        homeRefresh.setTargetAmount(amount("12000"));
        homeRefresh.setCurrentAmount(amount("9300"));
        homeRefresh.setTargetDate(LocalDate.now().plusMonths(5));
        homeRefresh.setIcon("sofa");
        homeRefresh.setColor("#3b82f6");
        homeRefresh.setStatus(GoalStatus.ACTIVE);

        goalRepository.saveAll(List.of(emergencyFund, vacation, homeRefresh));
    }

    private void createTransfer(UserEntity user, AccountEntity from, AccountEntity to, BigDecimal amount, LocalDate date, String note) {
        TransactionEntity transfer = new TransactionEntity();
        transfer.setUser(user);
        transfer.setAccount(from);
        transfer.setDestinationAccount(to);
        transfer.setType(TransactionType.TRANSFER);
        transfer.setAmount(amount);
        transfer.setTransactionDate(date);
        transfer.setMerchant("Internal Transfer");
        transfer.setNote(note);
        transfer.setPaymentMethod("BANK");
        transfer.setTags(new LinkedHashSet<>(List.of("savings")));
        from.setCurrentBalance(from.getCurrentBalance().subtract(amount));
        to.setCurrentBalance(to.getCurrentBalance().add(amount));
        transactionRepository.save(transfer);
    }

    private void createTransaction(
            UserEntity user,
            AccountEntity account,
            AccountEntity destinationAccount,
            CategoryEntity category,
            RecurringTransactionEntity recurring,
            TransactionType type,
            BigDecimal amount,
            LocalDate date,
            String merchant,
            String note,
            String paymentMethod,
            String... tags
    ) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setUser(user);
        transaction.setAccount(account);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setCategory(category);
        transaction.setRecurringTransaction(recurring);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setTransactionDate(date);
        transaction.setMerchant(merchant);
        transaction.setNote(note);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setTags(new LinkedHashSet<>(List.of(tags)));

        if (type == TransactionType.INCOME) {
            account.setCurrentBalance(account.getCurrentBalance().add(amount));
        } else if (type == TransactionType.EXPENSE) {
            account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
        } else if (destinationAccount != null) {
            account.setCurrentBalance(account.getCurrentBalance().subtract(amount));
            destinationAccount.setCurrentBalance(destinationAccount.getCurrentBalance().add(amount));
        }

        transactionRepository.save(transaction);
    }

    private LocalDate nextRunDate(int preferredDayOfMonth) {
        LocalDate today = LocalDate.now();
        LocalDate candidate = YearMonth.from(today).atDay(Math.min(preferredDayOfMonth, YearMonth.from(today).lengthOfMonth()));
        if (candidate.isBefore(today)) {
            YearMonth nextMonth = YearMonth.from(today).plusMonths(1);
            return nextMonth.atDay(Math.min(preferredDayOfMonth, nextMonth.lengthOfMonth()));
        }
        return candidate;
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(long value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
