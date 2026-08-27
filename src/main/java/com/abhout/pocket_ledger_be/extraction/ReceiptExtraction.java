package com.abhout.pocket_ledger_be.extraction;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.math.BigDecimal;

public record ReceiptExtraction(
        @JsonPropertyDescription("Transaction date in ISO 8601 format (yyyy-MM-dd). Null if not clearly eligible")
        String date,
        @JsonPropertyDescription("merchant or payee name. Nullif not clearly legible.")
        String merchant,
        @JsonPropertyDescription("Total transaction amount as a positive number. Null if not clearly legible.")
        BigDecimal amount,
        @JsonPropertyDescription("Either \"expense\" or \"income\". Null if not clearly determinable.")
        String type,
        @JsonPropertyDescription("Category name, only if it confidently matches one of the provided known categories. Null otherwise.")
        String category
) {}
