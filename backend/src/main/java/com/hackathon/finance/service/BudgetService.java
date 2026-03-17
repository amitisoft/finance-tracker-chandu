package com.hackathon.finance.service;

import com.hackathon.finance.dto.budget.BudgetRequest;
import com.hackathon.finance.dto.budget.BudgetResponse;
import com.hackathon.finance.entity.BudgetEntity;
import com.hackathon.finance.entity.CategoryEntity;
import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.entity.enums.TransactionType;
import com.hackathon.finance.exception.ConflictException;
import com.hackathon.finance.exception.NotFoundException;
import com.hackathon.finance.mapper.EntityMapper;
import com.hackathon.finance.repository.BudgetRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final UserContextService userContextService;
    private final TransactionService transactionService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(int month, int year) {
        UserEntity user = userContextService.getCurrentUser();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<TransactionEntity> transactions = transactionService.findTransactionsForRange(start, end);
        return budgetRepository.findAllByUserAndMonthAndYearOrderByCategory_NameAsc(user, month, year)
                .stream()
                .map(budget -> mapper.toBudgetResponse(budget, calculateActualSpent(transactions, budget.getCategory().getId())))
                .toList();
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        UserEntity user = userContextService.getCurrentUser();
        CategoryEntity category = categoryService.findOwned(request.categoryId());
        if (budgetRepository.existsByUserAndCategoryAndMonthAndYear(user, category, request.month(), request.year())) {
            throw new ConflictException("Budget already exists for this category and month.");
        }
        BudgetEntity budget = new BudgetEntity();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonth(request.month());
        budget.setYear(request.year());
        budget.setAmount(request.amount());
        budget.setAlertThresholdPercent(request.alertThresholdPercent());
        budgetRepository.save(budget);
        return mapper.toBudgetResponse(budget, BigDecimal.ZERO);
    }

    @Transactional
    public BudgetResponse update(UUID id, BudgetRequest request) {
        BudgetEntity budget = findOwned(id);
        budget.setCategory(categoryService.findOwned(request.categoryId()));
        budget.setMonth(request.month());
        budget.setYear(request.year());
        budget.setAmount(request.amount());
        budget.setAlertThresholdPercent(request.alertThresholdPercent());
        return mapper.toBudgetResponse(budget, BigDecimal.ZERO);
    }

    @Transactional
    public void delete(UUID id) {
        budgetRepository.delete(findOwned(id));
    }

    @Transactional(readOnly = true)
    public BudgetEntity findOwned(UUID id) {
        return budgetRepository.findByIdAndUser(id, userContextService.getCurrentUser())
                .orElseThrow(() -> new NotFoundException("Budget not found."));
    }

    private BigDecimal calculateActualSpent(List<TransactionEntity> transactions, UUID categoryId) {
        return transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.EXPENSE)
                .filter(transaction -> transaction.getCategory() != null && transaction.getCategory().getId().equals(categoryId))
                .map(TransactionEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
