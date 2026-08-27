package com.abhout.pocket_ledger_be.parsing.models;

import java.util.List;

public record StatementParseResult(
        List<TransactionCandidate> transactions,
        int unparseable,
        boolean mappingComplete
) {
    public static StatementParseResult review() {
        return new StatementParseResult(List.of(), 0, false);
    }
}
