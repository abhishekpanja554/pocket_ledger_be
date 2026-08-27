package com.abhout.pocket_ledger_be.parsing;

import com.abhout.pocket_ledger_be.parsing.parsers.DateParser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DateParserTest {
    @Test
    void parseNormalDate() {
        LocalDate res = DateParser.parse("2026-07-05", false);
        assertEquals(LocalDate.of(2026, 7, 5), res);
    }

    @Test
    void slashSeparatedAmbiguousDateUsesDayFirstFlag() {
        LocalDate result = DateParser.parse("05/07/2026", true);
        assertEquals(LocalDate.of(2026, 7, 5), result);
    }

    @Test
    void slashSeparatedAmbiguousDateUsesMonthFirstWhenDayFirstFalse() {
        LocalDate result = DateParser.parse("05/07/2026", false);
        assertEquals(LocalDate.of(2026, 5, 7), result);
    }

    @Test
    void unambiguousDayOverridesDayFirstFlag() {
        LocalDate result = DateParser.parse("25/03/2026", false);
        assertEquals(LocalDate.of(2026, 3, 25), result);
    }

    @Test
    void invalidCalendarDateDoesNotRollOver() {
        LocalDate result = DateParser.parse("31/02/2026", true);
        assertNull(result);
    }

    @Test
    void textualMonthWithFourDigitYear() {
        LocalDate result = DateParser.parse("5 Jul 2026", true);
        assertEquals(LocalDate.of(2026, 7, 5), result);
    }

    @Test
    void textualMonthWithTwoDigitYearAndDashSeparators() {
        LocalDate result = DateParser.parse("05-Jul-26", true);
        assertEquals(LocalDate.of(2026, 7, 5), result);
    }

    @Test
    void unparseableTextReturnsNullNotGuess() {
        LocalDate result = DateParser.parse("not a date", true);
        assertNull(result);
    }

    @Test
    void blankAndNullInputReturnNull() {
        assertNull(DateParser.parse("", true));
        assertNull(DateParser.parse(null, true));
    }
}
