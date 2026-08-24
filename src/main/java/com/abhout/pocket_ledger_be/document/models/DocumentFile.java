package com.abhout.pocket_ledger_be.document.models;

public record DocumentFile(String filename, String mimeType, byte[] content) {
}
