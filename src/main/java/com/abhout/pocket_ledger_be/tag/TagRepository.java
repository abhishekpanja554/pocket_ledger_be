package com.abhout.pocket_ledger_be.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, TagId> {
    List<Tag> findByUserIdOrderByNameAsc(UUID userId);
}
