package com.abhout.pocket_ledger_be.transaction;

import com.abhout.pocket_ledger_be.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    User user;
    LocalDate date;
    String merchant;
    String category;
    BigDecimal amount;
    TxType type;
    String account;
    @JdbcTypeCode(SqlTypes.JSON)
    List<String> tags;
    boolean receipt;
    TxSource source;
    String fingerprint;
    Instant createdAt = Instant.now();

    Transaction(
            User user,
            LocalDate date,
            String merchant,
            String category,
            BigDecimal amount,
            TxType type,
            String account,
            List<String> tags,
            boolean receipt,
            TxSource source,
            String fingerprint
    ) {
        this.user = user;
        this.date = date;
        this.merchant = merchant;
        this.category = category;
        this.amount = amount;
        this.type = type;
        this.account = account;
        this.tags = tags;
        this.receipt = receipt;
        this.source = source;
        this.fingerprint = fingerprint;
    }
}
