package com.abhout.pocket_ledger_be.auth.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidPasswordException  extends BaseException{

    public InvalidPasswordException(String message) {
        super("INVALID_PASSWORD", message, HttpStatus.BAD_REQUEST);
    }
}
