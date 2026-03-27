package com.hackathon.finance.dto.rule;

import com.hackathon.finance.entity.enums.RuleActionType;
import com.hackathon.finance.entity.enums.RuleConditionField;
import com.hackathon.finance.entity.enums.RuleOperator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RuleRequest(
        @NotNull RuleConditionField conditionField,
        @NotNull RuleOperator conditionOperator,
        @NotBlank String conditionValue,
        @NotNull RuleActionType actionType,
        @NotBlank String actionValue,
        Boolean active,
        @Min(1) @Max(999) Integer priority
) {
}
