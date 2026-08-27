package com.abhout.pocket_ledger_be.parsing.parsers;

import lombok.NoArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public final class CSVFileReader {
    public static List<List<String>> readRows(byte[] fileBytes) throws IOException {
        String text = new String(fileBytes, StandardCharsets.UTF_8);
        if(text.startsWith("\uFEFF"))
            text = text.substring(1);

        List<List<String>> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(text,
                CSVFormat.DEFAULT.builder()
                        .setIgnoreEmptyLines(false)
                        .get())) {
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                record.forEach(row::add);
                if (row.stream().anyMatch(cell -> !cell.trim().isEmpty())) {
                    rows.add(row);
                }
            }
        }
        return rows;
    }


}
