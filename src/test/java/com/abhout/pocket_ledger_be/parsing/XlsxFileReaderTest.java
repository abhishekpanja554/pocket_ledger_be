package com.abhout.pocket_ledger_be.parsing;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XlsxFileReaderTest {

    private byte[] toBytes(Workbook workbook) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void readsFirstSheetWithData() throws IOException {
        byte[] bytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Statement");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Narration");
            header.createCell(2).setCellValue("Amount");

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("01/06/2026");
            data.createCell(1).setCellValue("ATM WDL");
            data.createCell(2).setCellValue(500.0);

            bytes = toBytes(workbook);
        }

        List<List<String>> rows = XlsxFileReader.readFirstNonEmptySheet(bytes);

        assertEquals(2, rows.size());
        assertEquals(List.of("Date", "Narration", "Amount"), rows.get(0));
        assertEquals("01/06/2026", rows.get(1).get(0));
        assertEquals("ATM WDL", rows.get(1).get(1));
        // DataFormatter should render a whole-number numeric cell without a
        // trailing ".0" — if POI actually renders "500.0" instead, that's real
        // information about DataFormatter's behavior, not a broken test.
        assertEquals("500", rows.get(1).get(2));
    }

    @Test
    void skipsEmptySheetsAndUsesFirstNonEmptyOne() throws IOException {
        byte[] bytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Empty Sheet"); // no rows at all

            Sheet realSheet = workbook.createSheet("Real Data");
            Row header = realSheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Narration");
            header.createCell(2).setCellValue("Amount");

            bytes = toBytes(workbook);
        }

        List<List<String>> rows = XlsxFileReader.readFirstNonEmptySheet(bytes);

        assertEquals(1, rows.size());
        assertEquals("Date", rows.get(0).get(0));
    }

    @Test
    void allSheetsEmptyReturnsEmptyList() throws IOException {
        byte[] bytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Empty1");
            workbook.createSheet("Empty2");
            bytes = toBytes(workbook);
        }

        assertTrue(XlsxFileReader.readFirstNonEmptySheet(bytes).isEmpty());
    }
}
