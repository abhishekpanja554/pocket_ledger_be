package com.abhout.pocket_ledger_be.document;

import com.abhout.pocket_ledger_be.auth.exceptions.BaseException;
import org.springframework.http.HttpStatus;

public class DocumentPurgeFailedException extends BaseException {
    public DocumentPurgeFailedException(String message) {
        super("DOCUMENT_PURGE_FAILED", message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
