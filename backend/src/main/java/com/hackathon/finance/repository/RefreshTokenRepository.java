package com.hackathon.finance.repository;

import com.hackathon.finance.entity.RefreshTokenEntity;
import com.hackathon.finance.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
    List<RefreshTokenEntity> findAllByUserAndRevokedFalse(UserEntity user);
}
