package com.hackathon.finance.repository;

import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.AccountMemberEntity;
import com.hackathon.finance.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMemberRepository extends JpaRepository<AccountMemberEntity, UUID> {
    List<AccountMemberEntity> findAllByUser(UserEntity user);
    List<AccountMemberEntity> findAllByAccountOrderByCreatedAtAsc(AccountEntity account);
    Optional<AccountMemberEntity> findByAccountAndUser(AccountEntity account, UserEntity user);
    boolean existsByAccountAndUser(AccountEntity account, UserEntity user);
}
