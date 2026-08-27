package com.abhout.pocket_ledger_be.extraction;

import com.abhout.pocket_ledger_be.auth.exceptions.BaseException;
import org.springframework.http.HttpStatus;

public class ReceiptExtractionRefusedException extends BaseException {
    public ReceiptExtractionRefusedException() {
        super(
                "EXTRACTION_REFUSED",
                "This document could not be processed automatically.",
                HttpStatus.UNPROCESSABLE_CONTENT
        );
    }
}
