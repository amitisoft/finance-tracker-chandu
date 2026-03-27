package com.hackathon.finance.repository;

import com.hackathon.finance.entity.AccountEntity;
import com.hackathon.finance.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findAllByUserOrderByCreatedAtDesc(UserEntity user);
    Optional<AccountEntity> findByIdAndUser(UUID id, UserEntity user);

    @Query("""
            select distinct a from AccountEntity a
            left join AccountMemberEntity am on am.account = a
            where a.user = :user or am.user = :user
            order by a.createdAt desc
            """)
    List<AccountEntity> findAllAccessibleByUser(@Param("user") UserEntity user);

    List<AccountEntity> findAllByIdIn(Set<UUID> ids);
}
