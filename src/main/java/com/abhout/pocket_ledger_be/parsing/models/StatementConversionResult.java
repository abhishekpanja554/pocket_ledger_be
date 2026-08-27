package com.abhout.pocket_ledger_be.parsing.models;

import java.util.List;

public record StatementConversionResult(
        List<TransactionCandidate> transactions,
        int unparseable
) {
}
