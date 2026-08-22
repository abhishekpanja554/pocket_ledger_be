package com.abhout.pocket_ledger_be.transaction.exceptions;

import com.abhout.pocket_ledger_be.auth.exceptions.BaseException;
import org.springframework.http.HttpStatus;

public class TransactionValidationException extends BaseException {
    public TransactionValidationException(String message) {
        super(
                "TRANSACTION_VALIDATION_FAILED",
                message,
                HttpStatus.BAD_REQUEST
        );
    }
}
