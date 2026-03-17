package com.hackathon.finance.service;

import com.hackathon.finance.dto.recurring.RecurringRequest;
import com.hackathon.finance.dto.recurring.RecurringResponse;
import com.hackathon.finance.entity.RecurringTransactionEntity;
import com.hackathon.finance.entity.enums.RecurringFrequency;
import com.hackathon.finance.exception.BadRequestException;
import com.hackathon.finance.exception.NotFoundException;
import com.hackathon.finance.mapper.EntityMapper;
import com.hackathon.finance.repository.RecurringTransactionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecurringService {

    private final RecurringTransactionRepository recurringRepository;
    private final UserContextService userContextService;
    private final CategoryService categoryService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<RecurringResponse> getAll() {
        return recurringRepository.findAllByUserOrderByNextRunDateAsc(userContextService.getCurrentUser()).stream().map(mapper::toRecurringResponse).toList();
    }

    @Transactional
    public RecurringResponse create(RecurringRequest request) {
        RecurringTransactionEntity recurring = new RecurringTransactionEntity();
        recurring.setUser(userContextService.getCurrentUser());
        applyRequest(recurring, request);
        recurring.setNextRunDate(request.startDate());
        recurringRepository.save(recurring);
        return mapper.toRecurringResponse(recurring);
    }

    @Transactional
    public RecurringResponse update(UUID id, RecurringRequest request) {
        RecurringTransactionEntity recurring = findOwned(id);
        applyRequest(recurring, request);
        if (recurring.getNextRunDate().isBefore(recurring.getStartDate())) {
            recurring.setNextRunDate(recurring.getStartDate());
        }
        return mapper.toRecurringResponse(recurring);
    }

    @Transactional
    public void delete(UUID id) {
        recurringRepository.delete(findOwned(id));
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processRecurringTransactions() {
        LocalDate today = LocalDate.now();
        recurringRepository.findAllByPausedFalseAndAutoCreateTransactionTrueAndNextRunDateLessThanEqual(today)
                .forEach(recurring -> {
                    if (recurring.getEndDate() != null && recurring.getNextRunDate().isAfter(recurring.getEndDate())) {
                        recurring.setPaused(true);
                        return;
                    }
                    transactionService.createFromRecurring(recurring);
                    recurring.setNextRunDate(calculateNextRunDate(recurring.getNextRunDate(), recurring.getFrequency()));
                    if (recurring.getEndDate() != null && recurring.getNextRunDate().isAfter(recurring.getEndDate())) {
                        recurring.setPaused(true);
                    }
                });
    }

    @Transactional(readOnly = true)
    public RecurringTransactionEntity findOwned(UUID id) {
        return recurringRepository.findByIdAndUser(id, userContextService.getCurrentUser())
                .orElseThrow(() -> new NotFoundException("Recurring transaction not found."));
    }

    private void applyRequest(RecurringTransactionEntity recurring, RecurringRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("Recurring end date cannot be before start date.");
        }
        recurring.setTitle(request.title().trim());
        recurring.setType(request.type());
        recurring.setAmount(request.amount());
        recurring.setCategory(request.categoryId() != null ? categoryService.findOwned(request.categoryId()) : null);
        recurring.setAccount(accountService.findOwned(request.accountId()));
        recurring.setFrequency(request.frequency());
        recurring.setStartDate(request.startDate());
        recurring.setEndDate(request.endDate());
        recurring.setAutoCreateTransaction(Boolean.TRUE.equals(request.autoCreateTransaction()));
        recurring.setPaused(Boolean.TRUE.equals(request.paused()));
    }

    private LocalDate calculateNextRunDate(LocalDate current, RecurringFrequency frequency) {
        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case YEARLY -> current.plusYears(1);
        };
    }
}
