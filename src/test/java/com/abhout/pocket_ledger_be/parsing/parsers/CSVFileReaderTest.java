package com.abhout.pocket_ledger_be.parsing.parsers;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CSVFileReaderTest {

    @Test
    void readsBasicRows() throws IOException {
        String csv = "Date,Narration,Amount\n01/06/2026,ATM WDL,500.00\n";
        List<List<String>> rows = CSVFileReader.readRows(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(2, rows.size());
        assertEquals(List.of("Date", "Narration", "Amount"), rows.get(0));
        assertEquals(List.of("01/06/2026", "ATM WDL", "500.00"), rows.get(1));
    }

    @Test
    void stripsUtf8Bom() throws IOException {
        String csv = "﻿Date,Narration,Amount\n01/06/2026,ATM WDL,500.00\n";
        List<List<String>> rows = CSVFileReader.readRows(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals("Date", rows.get(0).get(0)); // not "﻿Date"
    }

    @Test
    void skipsBlankRows() throws IOException {
        String csv = "Date,Narration,Amount\n\n01/06/2026,ATM WDL,500.00\n\n";
        List<List<String>> rows = CSVFileReader.readRows(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(2, rows.size());
    }

    @Test
    void preservesNonAsciiMerchantNames() throws IOException {
        // proves the UTF-8 fix actually matters, not just "compiles"
        String csv = "Date,Narration,Amount\n01/06/2026,Café ₹500,500.00\n";
        List<List<String>> rows = CSVFileReader.readRows(csv.getBytes(StandardCharsets.UTF_8));

        assertEquals("Café ₹500", rows.get(1).get(1));
    }
}
