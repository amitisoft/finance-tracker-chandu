package com.hackathon.finance.mapper;

import com.hackathon.finance.dto.account.AccountResponse;
import com.hackathon.finance.dto.auth.AuthResponse;
import com.hackathon.finance.dto.budget.BudgetResponse;
import com.hackathon.finance.dto.category.CategoryResponse;
import com.hackathon.finance.dto.goal.GoalResponse;
import com.hackathon.finance.dto.recurring.RecurringResponse;
import com.hackathon.finance.dto.transaction.TransactionResponse;
import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.BudgetEntity;
import com.hackathon.finance.entity.CategoryEntity;
import com.hackathon.finance.entity.GoalEntity;
import com.hackathon.finance.entity.RecurringTransactionEntity;
import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.UserEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public AuthResponse.UserSummary toUserSummary(UserEntity user) {
        return new AuthResponse.UserSummary(user.getId(), user.getEmail(), user.getDisplayName());
    }

    public AccountResponse toAccountResponse(AccountEntity account) {
        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType(),
                account.getOpeningBalance(),
                account.getCurrentBalance(),
                account.getInstitutionName(),
                account.getUpdatedAt()
        );
    }

    public CategoryResponse toCategoryResponse(CategoryEntity category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getColor(),
                category.getIcon(),
                category.isArchived()
        );
    }

    public TransactionResponse toTransactionResponse(TransactionEntity transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getId() : null,
                transaction.getDestinationAccount() != null ? transaction.getDestinationAccount().getName() : null,
                transaction.getCategory() != null ? transaction.getCategory().getId() : null,
                transaction.getCategory() != null ? transaction.getCategory().getName() : null,
                transaction.getMerchant(),
                transaction.getNote(),
                transaction.getPaymentMethod(),
                new LinkedHashSet<>(transaction.getTags()),
                transaction.getCreatedAt()
        );
    }

    public BudgetResponse toBudgetResponse(BudgetEntity budget, BigDecimal actualSpent) {
        BigDecimal remaining = budget.getAmount().subtract(actualSpent);
        double percentage = budget.getAmount().signum() == 0 ? 0
                : actualSpent.multiply(BigDecimal.valueOf(100)).divide(budget.getAmount(), 2, RoundingMode.HALF_UP).doubleValue();
        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getAmount(),
                actualSpent,
                remaining,
                percentage,
                budget.getMonth(),
                budget.getYear(),
                budget.getAlertThresholdPercent()
        );
    }

    public GoalResponse toGoalResponse(GoalEntity goal) {
        double progress = goal.getTargetAmount().signum() == 0 ? 0
                : goal.getCurrentAmount().multiply(BigDecimal.valueOf(100)).divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP).doubleValue();
        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                progress,
                goal.getTargetDate(),
                goal.getLinkedAccount() != null ? goal.getLinkedAccount().getId() : null,
                goal.getIcon(),
                goal.getColor(),
                goal.getStatus()
        );
    }

    public RecurringResponse toRecurringResponse(RecurringTransactionEntity recurring) {
        return new RecurringResponse(
                recurring.getId(),
                recurring.getTitle(),
                recurring.getType(),
                recurring.getAmount(),
                recurring.getCategory() != null ? recurring.getCategory().getId() : null,
                recurring.getCategory() != null ? recurring.getCategory().getName() : null,
                recurring.getAccount().getId(),
                recurring.getAccount().getName(),
                recurring.getFrequency(),
                recurring.getStartDate(),
                recurring.getEndDate(),
                recurring.getNextRunDate(),
                recurring.isAutoCreateTransaction(),
                recurring.isPaused()
        );
    }
}
