package com.hackathon.finance.repository;

import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.UserEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID>, JpaSpecificationExecutor<TransactionEntity> {
    Optional<TransactionEntity> findByIdAndUser(UUID id, UserEntity user);

    List<TransactionEntity> findTop5ByUserOrderByTransactionDateDescCreatedAtDesc(UserEntity user);

    List<TransactionEntity> findTop5ByAccountIdInOrderByTransactionDateDescCreatedAtDesc(Set<UUID> accountIds);

    @Query("""
            select t from TransactionEntity t
            where t.user = :user and t.transactionDate between :fromDate and :toDate
            """)
    List<TransactionEntity> findAllByUserAndDateRange(
            @Param("user") UserEntity user,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    List<TransactionEntity> findAllByAccountIdInAndTransactionDateBetween(Set<UUID> accountIds, LocalDate fromDate, LocalDate toDate);
}
