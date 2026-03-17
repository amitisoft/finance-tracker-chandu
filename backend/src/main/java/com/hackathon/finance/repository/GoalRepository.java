package com.hackathon.finance.repository;

import com.hackathon.finance.entity.GoalEntity;
import com.hackathon.finance.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {
    List<GoalEntity> findAllByUserOrderByCreatedAtDesc(UserEntity user);
    Optional<GoalEntity> findByIdAndUser(UUID id, UserEntity user);
}
