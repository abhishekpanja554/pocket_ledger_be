package com.abhout.pocket_ledger_be.transaction.models;

import com.abhout.pocket_ledger_be.transaction.enums.TxSource;
import com.abhout.pocket_ledger_be.transaction.enums.TxType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ValidTransaction(
        LocalDate date,
        String merchant,
        String category,
        BigDecimal amount,
        TxType type,
        String account,
        List<String> tags,
        boolean receipt,
        TxSource source
) {
    public  String fingerprint() {
        return date + "|" +
            merchant.trim().toLowerCase() + "|" +
            amount.toPlainString() + "|" +
            account.trim().toLowerCase();
    }
}
