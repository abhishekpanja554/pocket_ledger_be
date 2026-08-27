package com.abhout.pocket_ledger_be.parsing;

import com.abhout.pocket_ledger_be.parsing.models.StatementConversionResult;
import com.abhout.pocket_ledger_be.parsing.models.TransactionCandidate;
import com.abhout.pocket_ledger_be.transaction.models.TransactionValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementRowConverterTest {

    @Test
    void convertsDebitCreditColumnsToExpenseIncome() {
        Map<Integer, ColumnRole> mapping = Map.of(
                0, ColumnRole.DATE, 1, ColumnRole.MERCHANT,
                2, ColumnRole.DEBIT, 3, ColumnRole.CREDIT);
        List<List<String>> rows = List.of(
                List.of("01/06/2026", "ATM Withdrawal", "500.00", ""),
                List.of("02/06/2026", "Salary", "", "50000.00"));

        StatementConversionResult result = StatementRowConverter.convert(
                rows, mapping, List.of(), "Imported account", true);

        assertEquals(2, result.transactions().size());
        assertEquals(0, result.unparseable());

        TransactionCandidate expense = result.transactions().get(0);
        assertEquals("expense", expense.type());
        assertEquals(0, new BigDecimal("500.00").compareTo(expense.amount()));

        TransactionCandidate income = result.transactions().get(1);
        assertEquals("income", income.type());
        assertEquals(0, new BigDecimal("50000.00").compareTo(income.amount()));
    }

    @Test
    void signedAmountColumnDeterminesTypeWhenNoDebitCredit() {
        Map<Integer, ColumnRole> mapping = Map.of(
                0, ColumnRole.DATE, 1, ColumnRole.MERCHANT, 2, ColumnRole.AMOUNT);
        List<List<String>> rows = List.of(
                List.of("01/06/2026", "Grocery Store", "-1200.00"));

        StatementConversionResult result = StatementRowConverter.convert(
                rows, mapping, List.of(), "Imported account", true);

        assertEquals("expense", result.transactions().get(0).type());
        assertEquals(0, new BigDecimal("1200.00").compareTo(result.transactions().get(0).amount()));
    }

    @Test
    void rowMissingDateIsUnparseableNotSkippedSilently() {
        Map<Integer, ColumnRole> mapping = Map.of(
                0, ColumnRole.DATE, 1, ColumnRole.MERCHANT, 2, ColumnRole.AMOUNT);
        List<List<String>> rows = List.of(
                List.of("not a date", "Grocery Store", "-1200.00"));

        StatementConversionResult result = StatementRowConverter.convert(
                rows, mapping, List.of(), "Imported account", true);

        assertEquals(0, result.transactions().size());
        assertEquals(1, result.unparseable());
    }

    @Test
    void categoryOnlyAppliedWhenMatchingKnownCategoryCaseInsensitive() {
        Map<Integer, ColumnRole> mapping = Map.of(
                0, ColumnRole.DATE, 1, ColumnRole.MERCHANT,
                2, ColumnRole.AMOUNT, 3, ColumnRole.CATEGORY);
        List<List<String>> rows = List.of(
                List.of("01/06/2026", "Grocery Store", "-1200.00", "groceries"),
                List.of("02/06/2026", "Random Shop", "-300.00", "SomeUnknownCategory"));

        StatementConversionResult result = StatementRowConverter.convert(
                rows, mapping, List.of("Groceries", "Dining"), "Imported account", true);

        assertEquals("Groceries", result.transactions().get(0).category()); // matched, real casing preserved
        assertEquals(TransactionValidator.DEFAULT_CATEGORY, result.transactions().get(1).category());
    }

    @Test
    void accountFallsBackToProvidedDefaultWhenColumnMissing() {
        Map<Integer, ColumnRole> mapping = Map.of(
                0, ColumnRole.DATE, 1, ColumnRole.MERCHANT, 2, ColumnRole.AMOUNT);
        List<List<String>> rows = List.of(
                List.of("01/06/2026", "Grocery Store", "-1200.00"));

        StatementConversionResult result = StatementRowConverter.convert(
                rows, mapping, List.of(), "Drive import", true);

        assertEquals("Drive import", result.transactions().get(0).account());
    }
}
