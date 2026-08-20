package com.abhout.pocket_ledger_be.tag;

import com.abhout.pocket_ledger_be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity @Table(name = "tags") @IdClass(TagId.class)
public class Tag {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    User user;
    @Id String name;
    Instant createdAt = Instant.now();

    Tag(User user, String name) {
        this.user = user;
        this.name = name;
    }
}
