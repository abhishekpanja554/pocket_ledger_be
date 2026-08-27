package com.abhout.pocket_ledger_be.parsing.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionCandidate(
        LocalDate date, String merchant, BigDecimal amount,
        String type, String category, String account
) {}
