package com.abhout.pocket_ledger_be.parsing;

import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class XlsxFileReader {
    public static List<List<String>> readFirstNonEmptySheet(byte[] bytes) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new
                ByteArrayInputStream(bytes))) {
            DataFormatter formatter = new DataFormatter();
            for (Sheet sheet : workbook) {
                List<List<String>> rows = readSheet(sheet,
                        formatter);
                if (!rows.isEmpty()) return rows;
            }
            return List.of();
        }
    }

    private static List<List<String>> readSheet(Sheet sheet, DataFormatter formatter) {
        List<List<String>> rows = new ArrayList<>();
        for (Row row : sheet) {
            List<String> cells = new ArrayList<>();
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                cells.add(cell == null ? "" :
                        formatter.formatCellValue(cell));
            }
            if (cells.stream().anyMatch(c ->
                    !c.trim().isEmpty())) {
                rows.add(cells);
            }
        }
        return rows;
    }
}
