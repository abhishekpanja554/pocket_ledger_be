package com.abhout.pocket_ledger_be.storage;

import com.abhout.pocket_ledger_be.auth.exceptions.BaseException;
import org.springframework.http.HttpStatus;

public class StorageObjectNotFoundException extends BaseException {
    public StorageObjectNotFoundException() {
        super("STORAGE_OBJECT_NOT_FOUND",
                "The stored file is no longer available.",
                HttpStatus.NOT_FOUND
        );
    }
}
