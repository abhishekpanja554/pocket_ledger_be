package com.abhout.pocket_ledger_be.setting;

import com.abhout.pocket_ledger_be.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingRepository extends JpaRepository<Setting, SettingId> {
    List<Setting> findByUserId(UUID userId);
    Optional<Setting> findByUserIdAndKey(UUID userId, String key);
    List<Setting> findByKey(String key);
    @Query("SELECT s.user FROM Setting s WHERE s.key = :key")
    List<User> findUsersByKey(@Param("key") String key);
}
