package com.abhout.pocket_ledger_be.state;

import com.abhout.pocket_ledger_be.auth.exceptions.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidConfirmationException extends BaseException {
    public InvalidConfirmationException(String message) {
        super("INVALID_WIPE_CONFIRMATION", message, HttpStatus.BAD_REQUEST);
    }
}
