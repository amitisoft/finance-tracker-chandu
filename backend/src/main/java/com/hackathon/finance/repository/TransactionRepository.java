package com.hackathon.finance.repository;

import com.hackathon.finance.entity.TransactionEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.entity.enums.TransactionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    Optional<TransactionEntity> findByIdAndUser(UUID id, UserEntity user);

    @Query("""
            select t from TransactionEntity t
            where t.user = :user
              and (:fromDate is null or t.transactionDate >= :fromDate)
              and (:toDate is null or t.transactionDate <= :toDate)
              and (:accountId is null or t.account.id = :accountId)
              and (:categoryId is null or t.category.id = :categoryId)
              and (:type is null or t.type = :type)
              and (:searchTerm is null or lower(coalesce(t.merchant, '')) like lower(concat('%', :searchTerm, '%'))
                   or lower(coalesce(t.note, '')) like lower(concat('%', :searchTerm, '%')))
            order by t.transactionDate desc, t.createdAt desc
            """)
    List<TransactionEntity> search(
            @Param("user") UserEntity user,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("accountId") UUID accountId,
            @Param("categoryId") UUID categoryId,
            @Param("type") TransactionType type,
            @Param("searchTerm") String searchTerm
    );

    List<TransactionEntity> findTop5ByUserOrderByTransactionDateDescCreatedAtDesc(UserEntity user);

    @Query("""
            select t from TransactionEntity t
            where t.user = :user and t.transactionDate between :fromDate and :toDate
            """)
    List<TransactionEntity> findAllByUserAndDateRange(
            @Param("user") UserEntity user,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
