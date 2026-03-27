package com.hackathon.finance.service;

import com.hackathon.finance.dto.rule.RuleRequest;
import com.hackathon.finance.dto.rule.RuleResponse;
import com.hackathon.finance.entity.CategoryEntity;
import com.hackathon.finance.entity.RuleEntity;
import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.entity.enums.RuleActionType;
import com.hackathon.finance.entity.enums.RuleConditionField;
import com.hackathon.finance.entity.enums.RuleOperator;
import com.hackathon.finance.exception.BadRequestException;
import com.hackathon.finance.exception.NotFoundException;
import com.hackathon.finance.repository.RuleRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final UserContextService userContextService;
    private final CategoryService categoryService;

    @Transactional(readOnly = true)
    public List<RuleResponse> getRules() {
        return ruleRepository.findAllByUserOrderByPriorityAscCreatedAtAsc(userContextService.getCurrentUser()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RuleResponse create(RuleRequest request) {
        RuleEntity rule = new RuleEntity();
        rule.setUser(userContextService.getCurrentUser());
        apply(rule, request);
        ruleRepository.save(rule);
        return toResponse(rule);
    }

    @Transactional
    public RuleResponse update(UUID id, RuleRequest request) {
        RuleEntity rule = findOwned(id);
        apply(rule, request);
        return toResponse(rule);
    }

    @Transactional
    public void delete(UUID id) {
        ruleRepository.delete(findOwned(id));
    }

    @Transactional(readOnly = true)
    public RuleApplicationResult applyRules(UserEntity user, TransactionEntity transaction) {
        List<String> alerts = new ArrayList<>();
        LinkedHashSet<String> tags = new LinkedHashSet<>(transaction.getTags());
        for (RuleEntity rule : ruleRepository.findAllByUserAndActiveTrueOrderByPriorityAscCreatedAtAsc(user)) {
            if (!matches(rule, transaction)) {
                continue;
            }
            if (rule.getActionType() == RuleActionType.SET_CATEGORY) {
                CategoryEntity category = categoryService.findByUserAndName(user, rule.getActionValue())
                        .orElseThrow(() -> new BadRequestException("Rule category '" + rule.getActionValue() + "' was not found."));
                transaction.setCategory(category);
            } else if (rule.getActionType() == RuleActionType.ADD_TAG) {
                tags.add(rule.getActionValue().trim());
            } else if (rule.getActionType() == RuleActionType.TRIGGER_ALERT) {
                alerts.add(rule.getActionValue().trim());
            }
        }
        transaction.setTags(tags);
        return new RuleApplicationResult(alerts);
    }

    @Transactional(readOnly = true)
    public RuleEntity findOwned(UUID id) {
        return ruleRepository.findByIdAndUser(id, userContextService.getCurrentUser())
                .orElseThrow(() -> new NotFoundException("Rule not found."));
    }

    private void apply(RuleEntity rule, RuleRequest request) {
        rule.setConditionField(request.conditionField());
        rule.setConditionOperator(request.conditionOperator());
        rule.setConditionValue(request.conditionValue().trim());
        rule.setActionType(request.actionType());
        rule.setActionValue(request.actionValue().trim());
        rule.setActive(request.active() == null || request.active());
        rule.setPriority(request.priority() == null ? 100 : request.priority());
    }

    private boolean matches(RuleEntity rule, TransactionEntity transaction) {
        return switch (rule.getConditionField()) {
            case MERCHANT -> matchText(transaction.getMerchant(), rule.getConditionOperator(), rule.getConditionValue());
            case CATEGORY -> matchText(transaction.getCategory() != null ? transaction.getCategory().getName() : null,
                    rule.getConditionOperator(), rule.getConditionValue());
            case TYPE -> matchText(transaction.getType().name(), rule.getConditionOperator(), rule.getConditionValue());
            case AMOUNT -> matchAmount(transaction.getAmount(), rule.getConditionOperator(), rule.getConditionValue());
        };
    }

    private boolean matchText(String actual, RuleOperator operator, String expected) {
        if (actual == null) {
            return false;
        }
        String normalizedActual = actual.trim().toLowerCase();
        String normalizedExpected = expected.trim().toLowerCase();
        return switch (operator) {
            case EQUALS -> normalizedActual.equals(normalizedExpected);
            case CONTAINS -> normalizedActual.contains(normalizedExpected);
            default -> false;
        };
    }

    private boolean matchAmount(BigDecimal actual, RuleOperator operator, String expected) {
        BigDecimal threshold = new BigDecimal(expected.trim());
        return switch (operator) {
            case EQUALS -> actual.compareTo(threshold) == 0;
            case GREATER_THAN -> actual.compareTo(threshold) > 0;
            case LESS_THAN -> actual.compareTo(threshold) < 0;
            default -> false;
        };
    }

    private RuleResponse toResponse(RuleEntity rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getConditionField(),
                rule.getConditionOperator(),
                rule.getConditionValue(),
                rule.getActionType(),
                rule.getActionValue(),
                rule.isActive(),
                rule.getPriority()
        );
    }

    public record RuleApplicationResult(List<String> alerts) {
    }
}
