package com.hackathon.finance.repository;

import com.hackathon.finance.entity.BudgetEntity;
import com.hackathon.finance.entity.CategoryEntity;
import com.hackathon.finance.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<BudgetEntity, UUID> {
    List<BudgetEntity> findAllByUserAndMonthAndYearOrderByCategory_NameAsc(UserEntity user, int month, int year);
    Optional<BudgetEntity> findByIdAndUser(UUID id, UserEntity user);
    boolean existsByUserAndCategoryAndMonthAndYear(UserEntity user, CategoryEntity category, int month, int year);
}
