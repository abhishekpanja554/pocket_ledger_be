package com.abhout.pocket_ledger_be.transaction.exceptions;

import com.abhout.pocket_ledger_be.auth.exceptions.BaseException;
import org.springframework.http.HttpStatus;

public class TransactionConflictException extends BaseException {
    public TransactionConflictException() {
        super(
            "TRANSACTION_CONFLICT",
            "Another transaction already has that date, merchant, amount and account.",
            HttpStatus.CONFLICT
        );
    }
}
