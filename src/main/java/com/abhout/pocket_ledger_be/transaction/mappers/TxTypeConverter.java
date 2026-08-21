package com.abhout.pocket_ledger_be.transaction.mappers;

import com.abhout.pocket_ledger_be.transaction.enums.TxType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TxTypeConverter implements AttributeConverter<TxType, String> {
    @Override
    public String convertToDatabaseColumn(TxType attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public TxType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TxType.valueOf(dbData.toUpperCase());
    }
}
