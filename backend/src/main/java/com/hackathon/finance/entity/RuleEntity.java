package com.hackathon.finance.entity;

import com.hackathon.finance.entity.enums.RuleActionType;
import com.hackathon.finance.entity.enums.RuleConditionField;
import com.hackathon.finance.entity.enums.RuleOperator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rules")
public class RuleEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleConditionField conditionField;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleOperator conditionOperator;

    @Column(nullable = false, length = 255)
    private String conditionValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RuleActionType actionType;

    @Column(nullable = false, length = 255)
    private String actionValue;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int priority = 100;
}
