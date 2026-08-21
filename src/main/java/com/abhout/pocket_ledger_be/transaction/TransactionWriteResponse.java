package com.abhout.pocket_ledger_be.transaction;

import java.util.List;

public record TransactionWriteResponse(
        int inserted,
        int duplicates,
        int skipped,
        int needsReview,
        List<String> errors,
        List<TransactionResponse> rows
) {
}
