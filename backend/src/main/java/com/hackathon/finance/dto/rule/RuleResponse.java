package com.hackathon.finance.dto.rule;

import com.hackathon.finance.entity.enums.RuleActionType;
import com.hackathon.finance.entity.enums.RuleConditionField;
import com.hackathon.finance.entity.enums.RuleOperator;
import java.util.UUID;

public record RuleResponse(
        UUID id,
        RuleConditionField conditionField,
        RuleOperator conditionOperator,
        String conditionValue,
        RuleActionType actionType,
        String actionValue,
        boolean active,
        int priority
) {
}
