package com.abhout.pocket_ledger_be.auth.exceptions;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends BaseException {
    public EmailAlreadyExistsException(String email) {
        super("USER_ALREADY_EXISTS","An account with email "
                + email
                + " already exists", HttpStatus.CONFLICT);
    }
}
