package com.abhout.pocket_ledger_be.storage;

import com.abhout.pocket_ledger_be.auth.exceptions.BaseException;
import org.springframework.http.HttpStatus;

public class DocumentNotFoundException extends BaseException {
    public DocumentNotFoundException() {
        super(
                "DOCUMENT_NOT_FOUND",
                "The document is no longer available.",
                HttpStatus.NOT_FOUND
        );
    }
}
