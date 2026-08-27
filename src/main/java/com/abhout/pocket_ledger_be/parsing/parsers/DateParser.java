package com.abhout.pocket_ledger_be.parsing.parsers;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DateParser {
    private static final Pattern ISO =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern SLASH_DATE = Pattern.compile(
            "^(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{2}|\\d{4})$");
    private static final Pattern TEXTUAL_DATE = Pattern.compile(
            "^(\\d{1,2})[\\s-]([A-Za-z]{3,})[\\s-](\\d{2}|\\d{4})$");
    private static final List<String> MONTHS = List.of(
        "jan",
            "feb",
            "mar",
            "apr",
            "may",
            "jun",
            "jul",
            "aug",
            "sep",
            "oct",
            "nov",
            "dec"
    );

    private static int normalizeYear(String raw) {
        return raw.length() == 2 ? 2000 +
                Integer.parseInt(raw) : Integer.parseInt(raw);
    }

    private static LocalDate tryParseIso(String raw) {
        try { return LocalDate.parse(raw); } catch
        (DateTimeParseException e) { return null; }
    }

    private static LocalDate realDateOrNull(int year, int
            month, int day) {
        if (month < 1 || month > 12 || day < 1 || day > 31)
            return null;
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }

    public static LocalDate parse(String value, boolean
            dayFirst) {
        if (value == null) return null;
        String raw = value.trim();
        if (raw.isEmpty()) return null;
        if (ISO.matcher(raw).matches()) {
            return tryParseIso(raw);
        }

        Matcher slash = SLASH_DATE.matcher(raw);
        if (slash.matches()) {
            int a = Integer.parseInt(slash.group(1));
            int b = Integer.parseInt(slash.group(2));
            int year = normalizeYear(slash.group(3));
            int day, month;
            if (a > 12) { day = a; month = b; }
            else if (b > 12) { day = b; month = a; }
            else if (dayFirst) { day = a; month = b; }
            else { day = b; month = a; }
            return realDateOrNull(year, month, day);
        }

        Matcher textual = TEXTUAL_DATE.matcher(raw);
        if (textual.matches()) {
            int day = Integer.parseInt(textual.group(1));
            int monthIndex = MONTHS.indexOf(textual.group(2).substring(0,3)
                    .toLowerCase());
            int year = normalizeYear(textual.group(3));
            if (monthIndex >= 0) return realDateOrNull(year, monthIndex + 1, day);
        }

        return null;
    }
}
