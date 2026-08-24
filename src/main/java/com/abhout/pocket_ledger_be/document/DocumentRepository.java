package com.abhout.pocket_ledger_be.document;

import com.abhout.pocket_ledger_be.document.models.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findTop100ByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Document> findByIdAndUserId(UUID id, UUID userId);
}
