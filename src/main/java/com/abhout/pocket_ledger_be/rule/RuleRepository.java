package com.abhout.pocket_ledger_be.rule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {
    List<Rule> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
