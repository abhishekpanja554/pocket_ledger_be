package com.abhout.pocket_ledger_be.auth.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public abstract class BaseException extends RuntimeException {
    @Getter final private String errorCode;
    @Getter final private HttpStatus status;

    protected BaseException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
