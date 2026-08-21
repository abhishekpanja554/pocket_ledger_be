package com.abhout.pocket_ledger_be.transaction;

import com.abhout.pocket_ledger_be.transaction.enums.TxSource;
import com.abhout.pocket_ledger_be.transaction.enums.TxType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id, LocalDate date, String merchant,
        String category,
        BigDecimal amount,
        TxType type,
        String account,
        List<String> tags,
        boolean receipt,
        TxSource source,
        String fingerprint,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
            tx.getId(),
            tx.getDate(),
            tx.getMerchant(),
            tx.getCategory(),
            tx.getAmount(),
            tx.getType(),
            tx.getAccount(),
            tx.getTags(),
            tx.isReceipt(),
            tx.getSource(),
            tx.getFingerprint(),
            tx.getCreatedAt()
        );
    }
}
