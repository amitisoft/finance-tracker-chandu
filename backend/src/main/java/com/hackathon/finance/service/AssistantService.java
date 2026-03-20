package com.hackathon.finance.service;

import com.hackathon.finance.dto.account.AccountResponse;
import com.hackathon.finance.dto.assistant.AssistantRequest;
import com.hackathon.finance.dto.assistant.AssistantResponse;
import com.hackathon.finance.dto.budget.BudgetRequest;
import com.hackathon.finance.dto.budget.BudgetResponse;
import com.hackathon.finance.dto.category.CategoryResponse;
import com.hackathon.finance.dto.transaction.TransactionRequest;
import com.hackathon.finance.dto.transaction.TransactionResponse;
import com.hackathon.finance.entity.enums.AccountType;
import com.hackathon.finance.entity.enums.CategoryType;
import com.hackathon.finance.entity.enums.TransactionType;
import com.hackathon.finance.service.OpenAiAssistantClient.AssistantIntentResult;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssistantService {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?:for|of|rs|rupees?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:rs|rupees?|inr)?", Pattern.CASE_INSENSITIVE);
    private static final Set<String> EXPENSE_HINTS = Set.of("spent", "pay", "paid", "bought", "buy", "ate", "eat", "ordered", "purchase", "register", "record");
    private static final Set<String> EXPENSE_CATEGORIES = Set.of("rent", "food", "restaurant", "biryani", "meal", "transport", "uber", "metro", "cab", "utilities", "utility", "shopping", "subscription", "entertainment");

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final OpenAiAssistantClient openAiAssistantClient;

    @Transactional
    public AssistantResponse handle(AssistantRequest request) {
        try {
            String rawMessage = request.message().trim();
            String normalized = normalize(rawMessage);
            if (normalized.isBlank()) {
                return respond("I could not understand that. Please say it again more clearly.", "unrecognized", false);
            }

            AssistantIntentResult interpreted = interpret(rawMessage, normalized);
            if (interpreted != null && interpreted.needsClarification()) {
                return respond(
                        interpreted.clarificationQuestion() != null && !interpreted.clarificationQuestion().isBlank()
                                ? interpreted.clarificationQuestion()
                                : "I need a little more detail before I can do that safely.",
                        interpreted.intent(),
                        false
                );
            }

            String intent = interpreted != null && interpreted.intent() != null ? interpreted.intent() : inferIntent(normalized);
            return switch (intent) {
                case "CREATE_EXPENSE" -> handleExpense(rawMessage, normalized, interpreted);
                case "ACCOUNT_BALANCE" -> handleBalance(normalized, interpreted);
                case "REMAINING_BUDGET" -> handleBudget(normalized, interpreted);
                case "UPDATE_BUDGET" -> handleBudgetUpdate(normalized, interpreted);
                case "BUDGET_STATUS" -> handleBudgetStatus();
                case "EXPENSE_SUMMARY" -> handleExpenseSummary(normalized, interpreted);
                default -> respond(
                        "I can help you add an expense, tell you an account balance, manage budgets, or check weekly or monthly spending.",
                        "IRRELEVANT",
                        false
                );
            };
        } catch (RuntimeException exception) {
            return respond(
                    "I could not safely process that request. Please rephrase it with the amount, account, or category a little more clearly.",
                    "ASSISTANT_ERROR",
                    false
            );
        }
    }

    private AssistantIntentResult interpret(String rawMessage, String normalized) {
        try {
            AssistantIntentResult aiResult = openAiAssistantClient.interpret(rawMessage);
            if (aiResult != null) {
                return aiResult;
            }
        } catch (RuntimeException ignored) {
            // Fallback to local heuristics when the AI provider is unavailable or returns an invalid payload.
        }
        return heuristicInterpretation(normalized);
    }

    private AssistantIntentResult heuristicInterpretation(String normalized) {
        String intent = inferIntent(normalized);
        String amount = findAmount(normalized);
        String category = findCategoryName(normalized);
        String accountName = extractRequestedAccountPhrase(normalized);
        if (accountName == null) {
            accountName = findAccountName(normalized);
        }
        String budgetCategory = normalized.contains("budget") ? category : null;
        String merchant = inferMerchantFromNormalized(normalized);
        String timeRange = normalized.contains("week") ? "WEEK" : normalized.contains("month") ? "MONTH" : null;
        boolean needsClarification = "ACCOUNT_BALANCE".equals(intent) && accountName == null;
        String clarificationQuestion = needsClarification ? "Please mention the account name you want the balance for." : null;
        return new AssistantIntentResult(intent, needsClarification, clarificationQuestion, amount, merchant, category, accountName, budgetCategory, timeRange, inferBudgetOperation(normalized));
    }

    private AssistantResponse handleExpense(String rawMessage, String normalized, AssistantIntentResult interpreted) {
        String amountValue = interpreted != null && interpreted.amount() != null ? interpreted.amount() : findAmount(normalized);
        if (amountValue == null) {
            return respond("I heard an expense, but I could not detect the amount. Please say the amount clearly.", "CREATE_EXPENSE", false);
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountValue);
        } catch (NumberFormatException exception) {
            return respond("I heard an expense, but the amount was not clear enough. Please say the amount again.", "CREATE_EXPENSE", false);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return respond("The expense amount must be greater than zero.", "CREATE_EXPENSE", false);
        }

        List<CategoryResponse> categories = categoryService.getAll().stream()
                .filter(category -> category.type() == CategoryType.EXPENSE)
                .toList();
        String categoryHint = interpreted != null && interpreted.category() != null ? interpreted.category() : normalized;
        String explicitAccountName = interpreted != null && interpreted.accountName() != null ? interpreted.accountName() : extractRequestedAccountPhrase(normalized);
        CategoryResponse category = inferCategory(categoryHint, normalized, categories);
        if (explicitAccountName == null || explicitAccountName.isBlank()) {
            return respond("I understood the expense, but I need the account name before I register it.", "CREATE_EXPENSE", false);
        }
        AccountResponse account = resolveAccount(explicitAccountName, normalized, amount);

        if (account == null) {
            return respond("I understood the expense, but I could not choose a valid funding account. Please mention the account name.", "CREATE_EXPENSE", false);
        }
        if (category == null) {
            return respond("I understood the amount, but I could not map the expense to a category. Please mention the category or merchant more clearly.", "CREATE_EXPENSE", false);
        }

        String merchant = sanitizeMerchant(interpreted != null && interpreted.merchant() != null ? interpreted.merchant() : inferMerchant(rawMessage, normalized));
        if ("Rent".equalsIgnoreCase(category.name())) {
            merchant = null;
        }
        if (merchant != null && sameTokens(merchant, account.name())) {
            merchant = null;
        }
        String note = rawMessage.length() > 200 ? rawMessage.substring(0, 200) : rawMessage;

        TransactionResponse created = transactionService.create(new TransactionRequest(
                TransactionType.EXPENSE,
                amount,
                LocalDate.now(),
                account.id(),
                null,
                category.id(),
                merchant,
                note,
                "VOICE_ASSISTANT",
                Set.of("assistant", "voice"),
                null
        ));

        String merchantClause = created.merchant() != null && !created.merchant().isBlank() ? " for " + created.merchant() : "";
        return respond(
                "Registered " + created.categoryName() + " expense of " + created.amount() + " from " + created.accountName() + merchantClause + ".",
                "CREATE_EXPENSE",
                true
        );
    }

    private AssistantResponse handleBalance(String normalized, AssistantIntentResult interpreted) {
        String explicitAccountName = interpreted != null ? interpreted.accountName() : null;
        AccountResponse account = resolveNamedAccount(explicitAccountName, normalized);
        if (account == null) {
            if (explicitAccountName != null && !explicitAccountName.isBlank()) {
                return respond("I could not find an account matching " + explicitAccountName + ".", "ACCOUNT_BALANCE", false);
            }
            return respond("Please mention the account name you want the balance for.", "ACCOUNT_BALANCE", false);
        }
        return respond("The current balance in " + account.name() + " is " + account.currentBalance() + ".", "ACCOUNT_BALANCE", false);
    }

    private AssistantResponse handleBudget(String normalized, AssistantIntentResult interpreted) {
        LocalDate today = LocalDate.now();
        List<BudgetResponse> budgets = budgetService.getBudgets(today.getMonthValue(), today.getYear());
        if (budgets.isEmpty()) {
            return respond("You do not have any budgets for this month yet.", "REMAINING_BUDGET", false);
        }

        String requestedCategory = interpreted != null && interpreted.budgetCategory() != null ? interpreted.budgetCategory() : findCategoryName(normalized);
        BudgetResponse matched = requestedCategory == null
                ? null
                : budgets.stream()
                        .filter(budget -> sameTokens(budget.categoryName(), requestedCategory))
                        .findFirst()
                        .orElse(null);

        if (matched == null) {
            BigDecimal remaining = budgets.stream().map(BudgetResponse::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
            return respond("Your total remaining budget for this month is " + remaining + ".", "REMAINING_BUDGET", false);
        }
        return respond("The remaining budget for " + matched.categoryName() + " this month is " + matched.remaining() + ".", "REMAINING_BUDGET", false);
    }

    private AssistantResponse handleBudgetUpdate(String normalized, AssistantIntentResult interpreted) {
        LocalDate today = LocalDate.now();
        String amountValue = interpreted != null && interpreted.amount() != null ? interpreted.amount() : findAmount(normalized);
        if (amountValue == null) {
            return respond("Please mention how much you want to add, reduce, or set for the budget.", "UPDATE_BUDGET", false);
        }

        BigDecimal delta;
        try {
            delta = new BigDecimal(amountValue);
        } catch (NumberFormatException exception) {
            return respond("The budget amount was not clear enough. Please say the amount again.", "UPDATE_BUDGET", false);
        }
        if (delta.compareTo(BigDecimal.ZERO) <= 0) {
            return respond("Budget changes must use an amount greater than zero.", "UPDATE_BUDGET", false);
        }

        List<CategoryResponse> categories = categoryService.getAll().stream()
                .filter(category -> category.type() == CategoryType.EXPENSE)
                .toList();
        String requestedCategory = interpreted != null && interpreted.budgetCategory() != null ? interpreted.budgetCategory() : findCategoryName(normalized);
        CategoryResponse category = inferCategory(requestedCategory, normalized, categories);
        if (category == null) {
            return respond("Please mention which budget category you want to update.", "UPDATE_BUDGET", false);
        }

        List<BudgetResponse> budgets = budgetService.getBudgets(today.getMonthValue(), today.getYear());
        BudgetResponse existing = budgets.stream()
                .filter(budget -> budget.categoryId().equals(category.id()))
                .findFirst()
                .orElse(null);

        String operation = interpreted != null && interpreted.budgetOperation() != null ? interpreted.budgetOperation() : inferBudgetOperation(normalized);
        BigDecimal targetAmount;
        if (existing == null) {
            targetAmount = delta;
        } else if ("DECREASE".equalsIgnoreCase(operation)) {
            targetAmount = existing.amount().subtract(delta);
        } else if ("SET".equalsIgnoreCase(operation)) {
            targetAmount = delta;
        } else {
            targetAmount = existing.amount().add(delta);
        }

        if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return respond("That budget change would make the budget zero or negative. Please use a larger budget amount.", "UPDATE_BUDGET", false);
        }

        BudgetResponse saved = existing == null
                ? budgetService.create(new BudgetRequest(category.id(), today.getMonthValue(), today.getYear(), targetAmount, 80))
                : budgetService.update(existing.id(), new BudgetRequest(category.id(), today.getMonthValue(), today.getYear(), targetAmount, existing.alertThresholdPercent()));

        String verb = existing == null ? "Created" : "Updated";
        String statusClause = saved.actualSpent().compareTo(saved.amount()) > 0
                ? " This budget is still over by " + saved.actualSpent().subtract(saved.amount()) + "."
                : " Remaining budget is " + saved.remaining() + ".";
        return respond(verb + " " + saved.categoryName() + " budget for this month to " + saved.amount() + "." + statusClause, "UPDATE_BUDGET", true);
    }

    private AssistantResponse handleBudgetStatus() {
        LocalDate today = LocalDate.now();
        List<BudgetResponse> budgets = budgetService.getBudgets(today.getMonthValue(), today.getYear());
        if (budgets.isEmpty()) {
            return respond("You do not have any budgets for this month yet.", "BUDGET_STATUS", false);
        }

        List<BudgetResponse> exceeded = budgets.stream()
                .filter(budget -> budget.remaining().compareTo(BigDecimal.ZERO) < 0)
                .toList();
        if (!exceeded.isEmpty()) {
            String summary = exceeded.stream()
                    .map(budget -> budget.categoryName() + " by " + budget.remaining().abs())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            return respond("These budgets are exceeded: " + summary + ".", "BUDGET_STATUS", false);
        }

        List<BudgetResponse> nearLimit = budgets.stream()
                .filter(budget -> budget.percentageUsed() >= budget.alertThresholdPercent())
                .toList();
        if (!nearLimit.isEmpty()) {
            String summary = nearLimit.stream()
                    .map(budget -> budget.categoryName() + " at " + Math.round(budget.percentageUsed()) + "%")
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            return respond("No budget is exceeded right now. These are close to the limit: " + summary + ".", "BUDGET_STATUS", false);
        }

        return respond("No budget is exceeded right now.", "BUDGET_STATUS", false);
    }

    private AssistantResponse handleExpenseSummary(String normalized, AssistantIntentResult interpreted) {
        LocalDate today = LocalDate.now();
        boolean weekly = interpreted != null && "WEEK".equalsIgnoreCase(interpreted.timeRange()) || normalized.contains("week");
        LocalDate fromDate = weekly
                ? today.with(DayOfWeek.MONDAY)
                : today.with(TemporalAdjusters.firstDayOfMonth());
        List<TransactionResponse> transactions = transactionService.search(fromDate, today, null, null, TransactionType.EXPENSE, null);
        BigDecimal total = transactions.stream().map(TransactionResponse::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return respond("Your total expense for " + (weekly ? "this week" : "this month") + " is " + total + ".", "EXPENSE_SUMMARY", false);
    }

    private String inferIntent(String normalized) {
        if (normalized.contains("balance")) {
            return "ACCOUNT_BALANCE";
        }
        if (normalized.contains("budget")) {
            if (normalized.contains("exceed") || normalized.contains("over budget") || normalized.contains("near limit")) {
                return "BUDGET_STATUS";
            }
            if (normalized.contains("add") || normalized.contains("increase") || normalized.contains("reduce") || normalized.contains("decrease")
                    || normalized.contains("set") || normalized.contains("update") || normalized.contains("change") || normalized.contains("+")) {
                return "UPDATE_BUDGET";
            }
            return "REMAINING_BUDGET";
        }
        if (isExpenseIntent(normalized)) {
            return "CREATE_EXPENSE";
        }
        if (normalized.contains("expense") || normalized.contains("spent") || normalized.contains("spend")) {
            return "EXPENSE_SUMMARY";
        }
        return "IRRELEVANT";
    }

    private boolean isExpenseIntent(String normalized) {
        boolean hasAmount = findAmount(normalized) != null;
        boolean hasExpenseWords = EXPENSE_HINTS.stream().anyMatch(normalized::contains);
        boolean hasExpenseCategory = EXPENSE_CATEGORIES.stream().anyMatch(normalized::contains);
        boolean hasCreationWords = normalized.contains("add") || normalized.contains("register") || normalized.contains("record") || normalized.contains("log");
        return hasAmount && (hasExpenseWords || hasExpenseCategory || hasCreationWords);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s.]", " ")
                .replaceAll("\\b(hey|hi|hello|please|just|um|uh|actually|can you|could you)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String findAmount(String normalized) {
        Matcher matcher = AMOUNT_PATTERN.matcher(normalized);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String findCategoryName(String normalized) {
        if (normalized.contains("food") || normalized.contains("restaurant") || normalized.contains("biryani") || normalized.contains("meal")) {
            return "Food";
        }
        if (normalized.contains("grocery") || normalized.contains("groceries")) {
            return "Food";
        }
        if (normalized.contains("rent") || normalized.contains("room")) {
            return "Rent";
        }
        if (normalized.contains("cab") || normalized.contains("metro") || normalized.contains("uber") || normalized.contains("transport")) {
            return "Transport";
        }
        if (normalized.contains("electricity") || normalized.contains("water bill") || normalized.contains("utility") || normalized.contains("utilities")) {
            return "Utilities";
        }
        if (normalized.contains("shopping")) {
            return "Shopping";
        }
        if (normalized.contains("subscription")) {
            return "Subscriptions";
        }
        if (normalized.contains("movie") || normalized.contains("entertainment")) {
            return "Entertainment";
        }
        return null;
    }

    private CategoryResponse inferCategory(String categoryHint, String normalized, List<CategoryResponse> categories) {
        String explicitName = categoryHint != null ? categoryHint : findCategoryName(normalized);
        if (explicitName != null) {
            CategoryResponse matched = categories.stream()
                    .filter(category -> sameTokens(category.name(), explicitName))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        return categories.stream()
                .filter(category -> normalized.contains(category.name().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private AccountResponse resolveAccount(String explicitAccountName, String normalized, BigDecimal amount) {
        AccountResponse named = resolveNamedAccount(explicitAccountName, normalized);
        if (named != null) {
            return named;
        }
        return accountService.getAccounts().stream()
                .filter(account -> account.type() != AccountType.CREDIT_CARD)
                .filter(account -> account.currentBalance().compareTo(amount) >= 0)
                .max(Comparator.comparing(AccountResponse::currentBalance))
                .orElse(accountService.getAccounts().stream().findFirst().orElse(null));
    }

    private AccountResponse resolveNamedAccount(String explicitAccountName, String normalized) {
        List<AccountResponse> accounts = accountService.getAccounts();
        if (explicitAccountName != null && !explicitAccountName.isBlank()) {
            return accounts.stream()
                    .filter(account -> sameTokens(account.name(), explicitAccountName))
                    .findFirst()
                    .orElse(null);
        }
        String fallbackName = findAccountName(normalized);
        if (fallbackName != null) {
            return accounts.stream()
                    .filter(account -> sameTokens(account.name(), fallbackName))
                    .findFirst()
                    .orElse(null);
        }
        return accounts.stream()
                .filter(account -> normalized.contains(account.name().toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseGet(() -> accounts.stream()
                        .filter(account -> {
                            String[] tokens = account.name().toLowerCase(Locale.ROOT).split("\\s+");
                            int matches = 0;
                            for (String token : tokens) {
                                if (token.length() > 2 && normalized.contains(token)) {
                                    matches++;
                                }
                            }
                            return matches >= 2;
                        })
                        .findFirst()
                        .orElse(null));
    }

    private String findAccountName(String normalized) {
        return accountService.getAccounts().stream()
                .map(AccountResponse::name)
                .filter(name -> normalized.contains(name.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    private String extractRequestedAccountPhrase(String normalized) {
        Matcher matcher = Pattern.compile("\\b(?:for|from|in)\\s+([a-z0-9\\s]+?(?:account|card|vault|wallet))\\b").matcher(normalized);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String inferBudgetOperation(String normalized) {
        if (normalized.contains("decrease") || normalized.contains("reduce") || normalized.contains("cut")) {
            return "DECREASE";
        }
        if (normalized.contains("set") || normalized.contains("change to")) {
            return "SET";
        }
        if (normalized.contains("add") || normalized.contains("increase") || normalized.contains("+")) {
            return "INCREASE";
        }
        return null;
    }

    private String inferMerchant(String rawMessage, String normalized) {
        Matcher atMatcher = Pattern.compile("\\b(?:at|in|from)\\s+([a-zA-Z0-9 &'-]{2,80})\\b(?:\\s+for\\s+\\d|\\s+rs|\\s+rupees?|$)", Pattern.CASE_INSENSITIVE)
                .matcher(rawMessage.trim());
        if (atMatcher.find()) {
            return atMatcher.group(1).trim();
        }
        return inferMerchantFromNormalized(normalized);
    }

    private String inferMerchantFromNormalized(String normalized) {
        if (normalized.contains("restaurant")) {
            return "Restaurant";
        }
        if (normalized.contains("room")) {
            return "Room rent";
        }
        return null;
    }

    private String sanitizeMerchant(String merchant) {
        if (merchant == null || merchant.isBlank()) {
            return null;
        }
        String trimmed = merchant.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private boolean sameTokens(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        if (normalizedLeft.equals(normalizedRight)) {
            return true;
        }
        Set<String> ignoredTokens = Set.of("account", "bank", "card", "vault", "wallet");
        Set<String> leftTokens = Set.of(normalizedLeft.split("\\s+"));
        Set<String> rightTokens = Set.of(normalizedRight.split("\\s+"));
        long overlap = leftTokens.stream()
                .filter(token -> !ignoredTokens.contains(token))
                .filter(rightTokens::contains)
                .count();
        return overlap >= 2;
    }

    private AssistantResponse respond(String reply, String intent, boolean actionTaken) {
        return new AssistantResponse(reply, intent, actionTaken, reply);
    }
}
