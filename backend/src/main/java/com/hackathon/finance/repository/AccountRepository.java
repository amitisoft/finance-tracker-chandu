package com.hackathon.finance.repository;

import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findAllByUserOrderByCreatedAtDesc(UserEntity user);
    Optional<AccountEntity> findByIdAndUser(UUID id, UserEntity user);
}
