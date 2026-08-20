package com.abhout.pocket_ledger_be.tag;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
public class TagId implements Serializable {
    private UUID user;
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagId)) return false;
        TagId tagId = (TagId) o;
        return Objects.equals(user, tagId.user) && Objects.equals(name, tagId.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, name);
    }
}
