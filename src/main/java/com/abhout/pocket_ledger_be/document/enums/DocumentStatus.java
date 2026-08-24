package com.abhout.pocket_ledger_be.document.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentStatus {
    QUEUED("queued"),
    STORED("stored"),
    REVIEW("review");

    private final String wiredValue;

    DocumentStatus(String wiredValue) {
        this.wiredValue = wiredValue;
    }

    public static boolean isTabular(String mimeType, String
            filename) {
        String name = filename.toLowerCase();
        return mimeType.contains("csv")
                || mimeType.contains("spreadsheet")
                || mimeType.contains("excel")
                || name.endsWith(".csv") || name.endsWith(".tsv")
                || name.endsWith(".xlsx");
    }

    public static DocumentStatus defaultStatusFor(String mimeType, String filename) {
        return isTabular(mimeType, filename) ?
                DocumentStatus.STORED : DocumentStatus.REVIEW;
    }

    @JsonValue
    public String getWiredValue() {
        return  wiredValue;
    }
}
