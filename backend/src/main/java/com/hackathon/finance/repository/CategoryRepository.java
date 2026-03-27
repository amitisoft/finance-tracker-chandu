package com.hackathon.finance.repository;

import com.hackathon.finance.entity.CategoryEntity;
import com.hackathon.finance.entity.UserEntity;
import com.hackathon.finance.entity.enums.CategoryType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    List<CategoryEntity> findAllByUserAndArchivedFalseOrderByNameAsc(UserEntity user);
    List<CategoryEntity> findAllByUserAndTypeAndArchivedFalseOrderByNameAsc(UserEntity user, CategoryType type);
    Optional<CategoryEntity> findByIdAndUser(UUID id, UserEntity user);
    Optional<CategoryEntity> findByUserAndNameIgnoreCase(UserEntity user, String name);
    boolean existsByUserAndNameIgnoreCaseAndType(UserEntity user, String name, CategoryType type);
}
