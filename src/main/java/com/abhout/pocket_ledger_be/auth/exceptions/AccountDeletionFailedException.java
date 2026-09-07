package com.abhout.pocket_ledger_be.auth.exceptions;

import org.springframework.http.HttpStatus;

public class AccountDeletionFailedException extends BaseException{
    public AccountDeletionFailedException(String message) {
        super("ACCOUNT_DELETION_FAILED", message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
