package com.abhout.pocket_ledger_be.document.DTOs;

import java.util.List;

public record DocumentIploadResponse(
        List<DocumentResponse> documents,
        List<String> errors,
        String status
) {
    public static DocumentIploadResponse of(List<DocumentResponse> storedDocuments, List<String> errors){
        String status = errors.isEmpty() ? "complete" : storedDocuments.isEmpty() ? "failed" : "partial";
        return new DocumentIploadResponse(storedDocuments, errors, status);
    }
}
