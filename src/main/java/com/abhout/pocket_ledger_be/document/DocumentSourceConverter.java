package com.abhout.pocket_ledger_be.document;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;
import java.util.stream.Collectors;

@Converter(autoApply = true)
public class DocumentSourceConverter implements AttributeConverter<DocumentSource, String> {

    private static final Map<DocumentSource, String> TO_DB = Map.of(
            DocumentSource.UPLOAD, "upload",
            DocumentSource.GOOGLE_DRIVE, "google-drive"
    );

    private static final Map<String, DocumentSource> FROM_DB = TO_DB.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    @Override
    public String convertToDatabaseColumn(DocumentSource attribute) {
        return attribute == null ? null : TO_DB.get(attribute);
    }

    @Override
    public DocumentSource convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        DocumentSource result = FROM_DB.get(dbData);
        if (result == null) {
            throw new IllegalArgumentException("Unknown DocumentSource value from DB: " + dbData);
        }
        return result;
    }
}