package com.abhout.pocket_ledger_be.auth;

import com.abhout.pocket_ledger_be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {
    Optional<AuthToken> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);
    @Modifying
    @Query("update AuthToken t set t.usedAt = CURRENT_TIMESTAMP where t.user = :user and t.purpose = :purpose and t.usedAt is null")
    void invalidateActive(User user, TokenPurpose purpose);
}
