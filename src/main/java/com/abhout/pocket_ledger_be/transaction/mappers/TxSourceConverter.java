package com.abhout.pocket_ledger_be.transaction.mappers;

import com.abhout.pocket_ledger_be.transaction.enums.TxSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;
import java.util.stream.Collectors;

@Converter(autoApply = true)
public class TxSourceConverter implements AttributeConverter<TxSource, String> {
    private static final Map<TxSource, String> TO_DB = Map.of(
            TxSource.MANUAL, "manual",
            TxSource.CSV, "csv",
            TxSource.DOCUMENT, "document",
            TxSource.GOOGLE_DRIVE, "google-drive"
    );

    private static final Map<String, TxSource> FROM_DB = TO_DB.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    @Override
    public String convertToDatabaseColumn(TxSource attribute) {
        return attribute == null ? null : TO_DB.get(attribute);
    }

    @Override
    public TxSource convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        TxSource result = FROM_DB.get(dbData);
        if (result == null) {
            throw new IllegalArgumentException("Unknown TxSource value from DB: " + dbData);
        }
        return result;
    }
}
