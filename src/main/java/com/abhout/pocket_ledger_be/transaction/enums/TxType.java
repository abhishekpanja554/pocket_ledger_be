package com.abhout.pocket_ledger_be.transaction.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TxType {
    EXPENSE("expense"),
    INCOME("income");

    private final String wireValue;

    TxType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return  wireValue;
    }
}
