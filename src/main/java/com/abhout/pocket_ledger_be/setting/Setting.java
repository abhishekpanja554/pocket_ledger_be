package com.abhout.pocket_ledger_be.setting;

import com.abhout.pocket_ledger_be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "settings") @IdClass(SettingId.class)
public class Setting {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    User user;
    @Id String key;
    @JdbcTypeCode(SqlTypes.JSON) String value;
    Instant updatedAt = Instant.now();

    Setting(User user, String key, String value) {
        this.user = user;
        this.key = key;
        this.value = value;
    }
}