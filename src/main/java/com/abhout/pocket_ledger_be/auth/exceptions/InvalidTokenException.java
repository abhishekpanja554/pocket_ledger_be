package com.abhout.pocket_ledger_be.auth.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BaseException {
    public InvalidTokenException(String message) {
        super("INVALID_TOKEN",message, HttpStatus.BAD_REQUEST);
    }
}
