package com.abhout.pocket_ledger_be.parsing;

import com.abhout.pocket_ledger_be.parsing.models.StatementConversionResult;
import com.abhout.pocket_ledger_be.parsing.models.StatementParseResult;
import com.abhout.pocket_ledger_be.parsing.parsers.CSVFileReader;
import com.abhout.pocket_ledger_be.parsing.parsers.ColumnMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class StatementParsingService {
    private static final int HEADER_SEARCH_DEPTH = 15;

    public StatementParseResult parse(
            byte[] fileBytes,
            String mimeType,
            List<String> knownCategories,
            String fallbackAccount,
            boolean dayFirst
    ){
        List<List<String>> rows;
        try {
            rows = isSpreadsheet(mimeType)
                    ?
                    XlsxFileReader.readFirstNonEmptySheet(fileBytes)
                    : CSVFileReader.readRows(fileBytes);
        } catch (IOException e) {
            return StatementParseResult.review();
        }

        if (rows.isEmpty())
            return StatementParseResult.review();

        int headerRowIndex = ColumnMapper.findHeaderRow(rows,
                HEADER_SEARCH_DEPTH);
        if (headerRowIndex < 0)
            return StatementParseResult.review();

        Map<Integer, ColumnRole> mapping =
                ColumnMapper.guessMapping(rows.get(headerRowIndex));
        if (!ColumnMapper.mappingIsComplete(mapping))
            return StatementParseResult.review();

        List<List<String>> dataRows = rows.subList(headerRowIndex + 1, rows.size());
        StatementConversionResult converted = StatementRowConverter.convert(
                dataRows, mapping, knownCategories,
                fallbackAccount, dayFirst
        );

        return new
                StatementParseResult(
                        converted.transactions(),
                        converted.unparseable(),
                true);
    }

    private boolean isSpreadsheet(String mimeType) {
        return mimeType != null &&
                mimeType.contains("spreadsheet");
    }
}
