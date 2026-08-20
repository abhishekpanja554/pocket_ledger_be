package com.abhout.pocket_ledger_be.setting;

import com.abhout.pocket_ledger_be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingRepository extends JpaRepository<Setting, SettingId> {
    List<Setting> findByUserId(UUID userId);
}
