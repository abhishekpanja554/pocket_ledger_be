package com.abhout.pocket_ledger_be.transaction;

public class TransactionValidationException extends RuntimeException {
    public TransactionValidationException(String message) {
        super(message);
    }
}
