package com.abhout.pocket_ledger_be.drive;

import java.time.Instant;

public record DriveFile(
        String id,
        String name,
        String mimeType,
        Instant modifiedTime
) {
}
