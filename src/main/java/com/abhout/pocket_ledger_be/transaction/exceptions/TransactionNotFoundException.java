package com.abhout.pocket_ledger_be.transaction.exceptions;

import com.abhout.pocket_ledger_be.auth.exceptions.BaseException;
import org.springframework.http.HttpStatus;

public class TransactionNotFoundException extends BaseException {
    public TransactionNotFoundException() {
        super("TRANSACTION_NOT_FOUND",
                "No such transaction exists.",
                HttpStatus.NOT_FOUND
        );
    }
}
