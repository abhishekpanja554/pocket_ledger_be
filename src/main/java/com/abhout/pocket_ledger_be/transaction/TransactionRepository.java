package com.abhout.pocket_ledger_be.transaction;

import com.abhout.pocket_ledger_be.transaction.models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserId(UUID userId);
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);
    List<Transaction> findByUserIdAndFingerprintIn(UUID userId, Collection<String> fingerprint);
    Optional<Transaction> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndFingerprintAndIdNot(UUID userId, String fingerprint, UUID id);
}
