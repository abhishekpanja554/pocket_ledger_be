package com.abhout.pocket_ledger_be.parsing;

import com.abhout.pocket_ledger_be.parsing.parsers.ColumnMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColumnMapperTest {

    @Test
    void roleForHeader_valueDt_isDateNotAmount_hdfcBugRegression() {
        assertEquals(ColumnRole.DATE, ColumnMapper.roleForHeader("Value Dt"));
    }

    @Test
    void roleForHeader_withdrawalAmt_isDebitNotGenericAmount() {
        assertEquals(ColumnRole.DEBIT, ColumnMapper.roleForHeader("Withdrawal Amt."));
    }

    @Test
    void roleForHeader_availableBalance_isIgnoreNotAmount_runningBalanceRisk() {
        assertEquals(ColumnRole.IGNORE, ColumnMapper.roleForHeader("Available Balance"));
    }

    @Test
    void roleForHeader_closingBal_isIgnoreViaHasWordBalPath() {
        assertEquals(ColumnRole.IGNORE, ColumnMapper.roleForHeader("Closing Bal"));
    }

    @Test
    void guessMapping_twoColumnsBothMatchMerchant_onlyFirstAssignedSecondIgnored() {
        Map<Integer, ColumnRole> mapping = ColumnMapper.guessMapping(List.of("Narration", "Bank Name"));
        assertEquals(ColumnRole.MERCHANT, mapping.get(0));
        assertEquals(ColumnRole.IGNORE, mapping.get(1));
    }

    @Test
    void findHeaderRow_skipsPreambleRowsAndFindsRealHeader() {
        List<List<String>> rows = List.of(
                List.of("Account Holder: Jane Doe"),
                List.of(),
                List.of("Statement Period: 01-Jun-2026 to 30-Jun-2026"),
                List.of("Date", "Narration", "Withdrawal Amt.", "Deposit Amt.", "Closing Balance"),
                List.of("01/06/2026", "ATM WDL", "500.00", "", "10000.00")
        );

        int headerRowIndex = ColumnMapper.findHeaderRow(rows, 15);
        assertEquals(3, headerRowIndex);
    }

    @Test
    void findHeaderRow_noRecognizableHeaderInSearchWindow_returnsMinusOne() {
        List<List<String>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            rows.add(List.of("junk " + i, "noise", "filler"));
        }

        int headerRowIndex = ColumnMapper.findHeaderRow(rows, 15);
        assertEquals(-1, headerRowIndex);
    }
}
