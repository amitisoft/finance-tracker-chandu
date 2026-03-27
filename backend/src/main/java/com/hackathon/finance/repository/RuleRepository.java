package com.hackathon.finance.repository;

import com.hackathon.finance.entity.RuleEntity;
import com.hackathon.finance.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<RuleEntity, UUID> {
    List<RuleEntity> findAllByUserOrderByPriorityAscCreatedAtAsc(UserEntity user);
    List<RuleEntity> findAllByUserAndActiveTrueOrderByPriorityAscCreatedAtAsc(UserEntity user);
    Optional<RuleEntity> findByIdAndUser(UUID id, UserEntity user);
}
