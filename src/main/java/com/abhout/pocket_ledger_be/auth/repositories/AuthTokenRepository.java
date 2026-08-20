package com.abhout.pocket_ledger_be.auth.repositories;

import com.abhout.pocket_ledger_be.auth.models.AuthToken;
import com.abhout.pocket_ledger_be.auth.models.TokenPurpose;
import com.abhout.pocket_ledger_be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {
    Optional<AuthToken> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);
    List<AuthToken> findAllByUserAndPurposeAndUsedAtIsNull(User user, TokenPurpose purpose);
}
