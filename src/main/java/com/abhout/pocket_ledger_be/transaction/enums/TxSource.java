package com.abhout.pocket_ledger_be.transaction.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TxSource {
    MANUAL("manual"),
    CSV("csv"),
    DOCUMENT("document"),
    GOOGLE_DRIVE("google-drive");

    private final String wireValue;
    TxSource(String wireValue) { this.wireValue =
            wireValue; }

    @JsonValue
    public String getWireValue() { return wireValue; }
}
