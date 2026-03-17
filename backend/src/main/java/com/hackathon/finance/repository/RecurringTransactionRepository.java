package com.hackathon.finance.repository;

import com.hackathon.finance.entity.RecurringTransactionEntity;
import com.hackathon.finance.entity.UserEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransactionEntity, UUID> {
    List<RecurringTransactionEntity> findAllByUserOrderByNextRunDateAsc(UserEntity user);
    Optional<RecurringTransactionEntity> findByIdAndUser(UUID id, UserEntity user);
    List<RecurringTransactionEntity> findAllByPausedFalseAndAutoCreateTransactionTrueAndNextRunDateLessThanEqual(LocalDate date);
}
