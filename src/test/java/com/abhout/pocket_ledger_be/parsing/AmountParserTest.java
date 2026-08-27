package com.abhout.pocket_ledger_be.parsing;

import com.abhout.pocket_ledger_be.parsing.parsers.AmountParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmountParserTest {

    @Test
    void parsesIndianCommaGroupedRupeeAmount() {
        BigDecimal result = AmountParser.parse("₹1,57,500.00");
        assertEquals(0, new BigDecimal("157500.00").compareTo(result));
    }

    @Test
    void parsesRsPrefixedAmount() {
        BigDecimal result = AmountParser.parse("Rs. 2,499");
        assertEquals(0, new BigDecimal("2499").compareTo(result));
    }

    @Test
    void parsesDrSuffixAsNegative() {
        BigDecimal result = AmountParser.parse("5,000.00 Dr");
        assertEquals(0, new BigDecimal("-5000").compareTo(result));
    }

    @Test
    void parsesCrSuffixAsPositive() {
        BigDecimal result = AmountParser.parse("1,200 Cr");
        assertEquals(0, new BigDecimal("1200").compareTo(result));
    }

    @Test
    void parsesParenthesizedAsNegative() {
        BigDecimal result = AmountParser.parse("(250.00)");
        assertEquals(0, new BigDecimal("-250.00").compareTo(result));
    }

    @Test
    void parsesPlainSignedNegative() {
        BigDecimal result = AmountParser.parse("-250");
        assertEquals(0, new BigDecimal("-250").compareTo(result));
    }

    @Test
    void parsesPlainExplicitPositive() {
        BigDecimal result = AmountParser.parse("+250");
        assertEquals(0, new BigDecimal("250").compareTo(result));
    }

    @Test
    void parsesInrCurrencyCodePrefix() {
        BigDecimal result = AmountParser.parse("INR 99.50");
        assertEquals(0, new BigDecimal("99.50").compareTo(result));
    }

    @Test
    void blankStringReturnsNull() {
        assertNull(AmountParser.parse(""));
        assertNull(AmountParser.parse(" "));
    }

    @Test
    void nonNumericTextReturnsNullNotThrow() {
        assertNull(AmountParser.parse("N/A"));
    }

    @Test
    void nullInputReturnsNull() {
        assertNull(AmountParser.parse(null));
    }

}