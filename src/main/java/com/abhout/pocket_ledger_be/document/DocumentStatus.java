package com.abhout.pocket_ledger_be.document;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentStatus {
    QUEUED("queued"),
    STORED("stored"),
    REVIEW("review");

    private final String wiredValue;

    DocumentStatus(String wiredValue) {
        this.wiredValue = wiredValue;
    }

    @JsonValue
    public String getWiredValue() {
        return  wiredValue;
    }
}
