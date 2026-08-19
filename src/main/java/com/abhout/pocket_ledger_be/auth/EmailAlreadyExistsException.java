package com.abhout.pocket_ledger_be.auth;

public class EmailAlreadyExistsException extends RuntimeException {
    EmailAlreadyExistsException(String email) {
        super("An account with email " + email + " already exists");
    }
}
