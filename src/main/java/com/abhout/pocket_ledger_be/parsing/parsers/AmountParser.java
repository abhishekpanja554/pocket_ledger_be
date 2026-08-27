package com.abhout.pocket_ledger_be.parsing.parsers;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AmountParser {
    private static final Pattern DR_CR = Pattern.compile("(^|\\s)(dr|cr)\\.?($|\\s)",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern PARENS = Pattern.compile("^\\(.*\\)$");
    private static final Pattern CURRENCY_NOISE = Pattern.compile("₹|\\bINR\\b|\\bRs\\.?|[$£€¥]",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_NUMBER = Pattern.compile("^\\d*\\.?\\d+$");

    public static BigDecimal parse(String value){
        if (value == null) return null;
        String raw = value.trim();
        if (raw.isEmpty()) return null;

        boolean negative = false;
        Matcher drCr = DR_CR.matcher(raw);
        if (drCr.find()) {
            negative = "dr".equalsIgnoreCase(drCr.group(2));
            raw = drCr.replaceFirst(" ").trim();
        }
        if (PARENS.matcher(raw).matches()) {
            negative = true;
            raw = raw.substring(1, raw.length() - 1);
        }
        raw = CURRENCY_NOISE.matcher(raw).replaceAll("").trim();
        if (raw.startsWith("-")) {
            negative = true;
            raw = raw.substring(1);
        } else if (raw.startsWith("+")) {
            raw = raw.substring(1);
        }
        raw = raw.replace(",", "").trim();
        if (raw.isEmpty() || !VALID_NUMBER.matcher(raw).matches()) return null;
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            return null;
        }
        return negative ? parsed.negate() : parsed;
    }
}
