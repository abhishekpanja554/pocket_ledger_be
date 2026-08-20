package com.abhout.pocket_ledger_be.rule;

import com.abhout.pocket_ledger_be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "rules")
public class Rule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    User user;

    String whenText;

    String thenText;

    boolean enabled;

    Instant createdAt = Instant.now();

    Rule(
            User user,
            String whenText,
            String thenText,
            boolean enabled
    ){
        this.user = user;
        this.whenText = whenText;
        this.thenText = thenText;
        this.enabled = enabled;
    }
}
