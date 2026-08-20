package com.abhout.pocket_ledger_be.setting;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class SettingId implements Serializable {
    private UUID user;
    private String key;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SettingId)) return false;
        SettingId that = (SettingId) o;
        return Objects.equals(user, that.user) && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, key);
    }
}
