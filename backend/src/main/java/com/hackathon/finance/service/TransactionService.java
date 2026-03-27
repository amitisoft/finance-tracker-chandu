package com.hackathon.finance.service;

import com.hackathon.finance.dto.transaction.TransactionRequest;
import com.hackathon.finance.dto.transaction.TransactionResponse;
import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.CategoryEntity;
import com.hackathon.finance.entity.RecurringTransactionEntity;
import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.entity.enums.TransactionType;
import com.hackathon.finance.exception.BadRequestException;
import com.hackathon.finance.exception.NotFoundException;
import com.hackathon.finance.mapper.EntityMapper;
import com.hackathon.finance.repository.RecurringTransactionRepository;
import com.hackathon.finance.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final AccountAccessService accountAccessService;
    private final CategoryService categoryService;
    private final UserContextService userContextService;
    private final RecurringTransactionRepository recurringTransactionRepository;
    private final RuleService ruleService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<TransactionResponse> search(LocalDate fromDate, LocalDate toDate, UUID accountId, UUID categoryId, TransactionType type, String searchTerm) {
        UserEntity user = userContextService.getCurrentUser();
        String normalizedSearchTerm = blankToNull(searchTerm);
        Set<UUID> accessibleAccountIds = accountAccessService.getAccessibleAccountIds();
        Specification<TransactionEntity> specification = belongsToAccounts(accessibleAccountIds)
                .and(onOrAfter(fromDate))
                .and(onOrBefore(toDate))
                .and(hasAccount(accountId))
                .and(hasCategory(categoryId))
                .and(hasType(type))
                .and(matchesSearch(normalizedSearchTerm));
        List<TransactionEntity> transactions = transactionRepository.findAll(
                specification,
                Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("createdAt"))
        );
        return transactions.stream().map(transaction -> mapper.toTransactionResponse(transaction, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(UUID id) {
        return mapper.toTransactionResponse(findOwned(id), List.of());
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setUser(userContextService.getCurrentUser());
        RuleService.RuleApplicationResult ruleResult = populateAndValidate(transaction, request);
        applyBalanceEffect(transaction, true);
        return mapper.toTransactionResponse(transactionRepository.save(transaction), ruleResult.alerts());
    }

    @Transactional
    public TransactionResponse update(UUID id, TransactionRequest request) {
        TransactionEntity transaction = findOwned(id);
        applyBalanceEffect(transaction, false);
        RuleService.RuleApplicationResult ruleResult = populateAndValidate(transaction, request);
        applyBalanceEffect(transaction, true);
        return mapper.toTransactionResponse(transaction, ruleResult.alerts());
    }

    @Transactional
    public void delete(UUID id) {
        TransactionEntity transaction = findOwned(id);
        applyBalanceEffect(transaction, false);
        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionEntity> findTransactionsForRange(LocalDate fromDate, LocalDate toDate) {
        Set<UUID> accessibleIds = accountAccessService.getAccessibleAccountIds();
        return accessibleIds.isEmpty() ? List.of() : transactionRepository.findAllByAccountIdInAndTransactionDateBetween(accessibleIds, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> recent() {
        Set<UUID> accessibleIds = accountAccessService.getAccessibleAccountIds();
        return accessibleIds.isEmpty() ? List.of() : transactionRepository.findTop5ByAccountIdInOrderByTransactionDateDescCreatedAtDesc(accessibleIds)
                .stream()
                .map(transaction -> mapper.toTransactionResponse(transaction, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionEntity findOwned(UUID id) {
        return transactionRepository.findByIdAndUser(id, userContextService.getCurrentUser())
                .orElseThrow(() -> new NotFoundException("Transaction not found."));
    }

    @Transactional
    public void createFromRecurring(RecurringTransactionEntity recurring) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setUser(recurring.getUser());
        transaction.setAccount(recurring.getAccount());
        transaction.setCategory(recurring.getCategory());
        transaction.setRecurringTransaction(recurring);
        transaction.setType(recurring.getType());
        transaction.setAmount(recurring.getAmount());
        transaction.setTransactionDate(recurring.getNextRunDate());
        transaction.setMerchant(recurring.getTitle());
        transaction.setTags(Set.of("recurring"));
        applyBalanceEffect(transaction, true);
        transactionRepository.save(transaction);
    }

    private RuleService.RuleApplicationResult populateAndValidate(TransactionEntity transaction, TransactionRequest request) {
        AccountEntity account = accountService.findAccessible(request.accountId());
        accountAccessService.ensureEditor(account);
        AccountEntity destinationAccount = request.destinationAccountId() != null ? accountService.findAccessible(request.destinationAccountId()) : null;
        if (destinationAccount != null) {
            accountAccessService.ensureEditor(destinationAccount);
        }
        CategoryEntity category = request.categoryId() != null ? categoryService.findOwned(request.categoryId()) : null;
        RecurringTransactionEntity recurring = request.recurringTransactionId() != null
                ? recurringTransactionRepository.findByIdAndUser(request.recurringTransactionId(), userContextService.getCurrentUser()).orElse(null)
                : null;
        if (request.type() != TransactionType.TRANSFER && category == null) {
            throw new BadRequestException("Category is required for income and expense transactions.");
        }
        if (request.type() == TransactionType.TRANSFER) {
            if (destinationAccount == null) {
                throw new BadRequestException("Destination account is required for transfers.");
            }
            if (destinationAccount.getId().equals(account.getId())) {
                throw new BadRequestException("Transfer source and destination must be different.");
            }
        }
        transaction.setAccount(account);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setCategory(category);
        transaction.setRecurringTransaction(recurring);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setTransactionDate(request.date());
        transaction.setMerchant(request.merchant());
        transaction.setNote(request.note());
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setTags(request.tags() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.tags()));
        RuleService.RuleApplicationResult ruleResult = ruleService.applyRules(userContextService.getCurrentUser(), transaction);
        if (request.type() != TransactionType.TRANSFER && transaction.getCategory() == null) {
            throw new BadRequestException("Category is required for income and expense transactions.");
        }
        return ruleResult;
    }

    private void applyBalanceEffect(TransactionEntity transaction, boolean apply) {
        BigDecimal amount = apply ? transaction.getAmount() : transaction.getAmount().negate();
        switch (transaction.getType()) {
            case INCOME -> transaction.getAccount().setCurrentBalance(transaction.getAccount().getCurrentBalance().add(amount));
            case EXPENSE -> {
                if (apply && transaction.getAccount().getCurrentBalance().compareTo(transaction.getAmount()) < 0) {
                    throw new BadRequestException("Insufficient balance for this expense.");
                }
                transaction.getAccount().setCurrentBalance(transaction.getAccount().getCurrentBalance().subtract(amount));
            }
            case TRANSFER -> {
                if (transaction.getDestinationAccount() == null) {
                    throw new BadRequestException("Transfer requires a destination account.");
                }
                if (apply && transaction.getAccount().getCurrentBalance().compareTo(transaction.getAmount()) < 0) {
                    throw new BadRequestException("Insufficient balance for this transfer.");
                }
                transaction.getAccount().setCurrentBalance(transaction.getAccount().getCurrentBalance().subtract(amount));
                transaction.getDestinationAccount().setCurrentBalance(transaction.getDestinationAccount().getCurrentBalance().add(amount));
            }
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Specification<TransactionEntity> belongsToAccounts(Set<UUID> accountIds) {
        return (root, query, criteriaBuilder) -> root.get("account").get("id").in(accountIds);
    }

    private Specification<TransactionEntity> onOrAfter(LocalDate fromDate) {
        return (root, query, criteriaBuilder) ->
                fromDate == null ? null : criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), fromDate);
    }

    private Specification<TransactionEntity> onOrBefore(LocalDate toDate) {
        return (root, query, criteriaBuilder) ->
                toDate == null ? null : criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), toDate);
    }

    private Specification<TransactionEntity> hasAccount(UUID accountId) {
        return (root, query, criteriaBuilder) ->
                accountId == null ? null : criteriaBuilder.equal(root.get("account").get("id"), accountId);
    }

    private Specification<TransactionEntity> hasCategory(UUID categoryId) {
        return (root, query, criteriaBuilder) ->
                categoryId == null ? null : criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    private Specification<TransactionEntity> hasType(TransactionType type) {
        return (root, query, criteriaBuilder) ->
                type == null ? null : criteriaBuilder.equal(root.get("type"), type);
    }

    private Specification<TransactionEntity> matchesSearch(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null) {
                return null;
            }
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("merchant"), "")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("note"), "")), pattern)
            );
        };
    }
}
