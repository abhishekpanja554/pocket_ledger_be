package com.abhout.pocket_ledger_be.document.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DocumentSource {
    UPLOAD("upload"),
    GOOGLE_DRIVE("google-drive");

    private final String wireValue;

    DocumentSource(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return  wireValue;
    }
}
