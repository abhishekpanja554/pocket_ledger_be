package com.abhout.pocket_ledger_be.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        String mimeType,
        long size,
        DocumentStatus status,
        DocumentSource source,
        Instant createdAt
) {
    public static DocumentResponse from (Document doc) {
        return  new DocumentResponse(
            doc.getId(),
            doc.getFilename(),
            doc.getMimeType(),
            doc.getSize(),
            doc.getStatus(),
            doc.getSource(),
            doc.getCreatedAt()
        );
    }
}
