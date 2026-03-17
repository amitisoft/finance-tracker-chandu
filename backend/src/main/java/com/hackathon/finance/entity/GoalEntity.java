package com.hackathon.finance.entity;

import com.hackathon.finance.entity.enums.GoalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "goals")
public class GoalEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id")
    private AccountEntity linkedAccount;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    private LocalDate targetDate;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GoalStatus status = GoalStatus.ACTIVE;
}
