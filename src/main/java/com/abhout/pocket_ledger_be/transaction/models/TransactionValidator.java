package com.abhout.pocket_ledger_be.transaction.models;

import com.abhout.pocket_ledger_be.transaction.enums.TxSource;
import com.abhout.pocket_ledger_be.transaction.enums.TxType;
import com.abhout.pocket_ledger_be.transaction.exceptions.TransactionValidationException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TransactionValidator {
    private static final Pattern ISO_DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern SEPARATED_DATE =
            Pattern.compile("^(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4})$");
    private static final int MAX_TAGS = 25;
    public static final String DEFAULT_CATEGORY = "Needs review";
    public ValidTransaction validate(JsonNode raw) throws TransactionValidationException {
        LocalDate date = validateDate(raw);
        String merchant = validateMerchant(raw);
        BigDecimal amount = validateAmount(raw);
        TxType type = validateType(raw);
        String category = validateOptionalTrimmed(raw, "category", "Needs review");
        String account = validateOptionalTrimmed(raw, "account", "Imported account");
        TxSource source = validateSource(raw);
        List<String> tags = validateTags(raw);
        boolean receipt = validateReceipt(raw);

        return new ValidTransaction(
            date,
            merchant,
            category,
            amount,
            type,
            account,
            tags,
            receipt,
            source
        );
    }

    private LocalDate validateDate(JsonNode raw) {
        JsonNode node = raw.get("date");
        if (node == null || node.isNull() || !node.isString() || node.asString().isBlank()) {
            throw new TransactionValidationException("date is required");
        }
        String text = node.asString().trim();
        int tIndex = text.indexOf('T');
        if (tIndex > 0) {
            text = text.substring(0, tIndex);
        }
        if (ISO_DATE.matcher(text).matches()) {
            try {
                return LocalDate.parse(text);
            } catch (DateTimeParseException e) {
                throw new TransactionValidationException("date is not a valid calendar date: " + text);
            }
        }
        Matcher matcher = SEPARATED_DATE.matcher(text);
        if (matcher.matches()) {
            int first = Integer.parseInt(matcher.group(1));
            int second = Integer.parseInt(matcher.group(2));
            int year = Integer.parseInt(matcher.group(3));

            int day = first;
            int month = second;
            if (first > 12) {
                day = second;
                month = first;
            }

            try {
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                throw new TransactionValidationException("date is not a valid calendar date: " + text);
            }
        }

        throw new TransactionValidationException("date is not in a recognized format: " + text);
    }

    private String validateMerchant(JsonNode raw) {
        JsonNode node = raw.get("merchant");
        if (node == null || node.isNull() || !node.isString()) {
            throw new TransactionValidationException("merchant is required");
        }
        String trimmed = node.asString().trim();
        if (trimmed.isEmpty()) {
            throw new TransactionValidationException("merchant cannot be empty");
        }
        if (trimmed.length() > 200) {
            trimmed = trimmed.substring(0, 200);
        }
        return trimmed;
    }

    private BigDecimal validateAmount(JsonNode raw) {
        JsonNode node = raw.get("amount");
        if (node == null || node.isNull()) {
            throw new TransactionValidationException("amount is required");
        }

        BigDecimal amount;
        if (node.isNumber()) {
            amount = node.decimalValue();
        } else if (node.isString()) {
            try {
                amount = new BigDecimal(node.asString().trim());
            } catch (NumberFormatException e) {
                throw new TransactionValidationException("amount is not a valid number: " + node.asString());
            }
        } else {
            throw new TransactionValidationException("amount must be a number or numeric string");
        }

        amount = amount.abs().setScale(2, RoundingMode.HALF_UP);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionValidationException("amount must be greater than zero");
        }
        return amount;
    }

    private TxType validateType(JsonNode raw) {
        JsonNode node = raw.get("type");
        if (node == null || node.isNull() || !node.isString()) {
            throw new TransactionValidationException("type is required and must be 'income' or 'expense'");
        }
        String text = node.asString().trim();
        if ("income".equals(text)) {
            return TxType.INCOME;
        }
        if ("expense".equals(text)) {
            return TxType.EXPENSE;
        }
        throw new TransactionValidationException("type must be exactly 'income' or 'expense', got: " + text);
    }

    private String validateOptionalTrimmed(JsonNode raw, String field, String defaultValue) {
        JsonNode node = raw.get(field);
        if (node == null || node.isNull() || !node.isString()) {
            return defaultValue;
        }
        String trimmed = node.asString().trim();
        if (trimmed.isEmpty()) {
            return defaultValue;
        }
        if (trimmed.length() > 80) {
            trimmed = trimmed.substring(0, 80);
        }
        return trimmed;
    }

    private TxSource validateSource(JsonNode raw) {
        JsonNode node = raw.get("source");
        if (node == null || node.isNull() || !node.isString()) {
            return TxSource.MANUAL;
        }
        return switch (node.asString().trim()) {
            case "csv" -> TxSource.CSV;
            case "document" -> TxSource.DOCUMENT;
            case "google-drive" -> TxSource.GOOGLE_DRIVE;
            default -> TxSource.MANUAL;
        };
    }

    private List<String> validateTags(JsonNode raw) {
        JsonNode node = raw.get("tags");
        if (node == null || node.isNull() || !node.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        Set<String> seenLower = new LinkedHashSet<>();

        for (JsonNode item : node) {
            if (item == null || !item.isString()) {
                continue;
            }
            String trimmed = item.asString().trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lower = trimmed.toLowerCase();
            if (!seenLower.add(lower)) {
                continue;
            }
            result.add(trimmed);
            if (result.size() >= MAX_TAGS) {
                break;
            }
        }

        return result;
    }

    private boolean validateReceipt(JsonNode raw) {
        JsonNode node = raw.get("receipt");
        return node != null && node.isBoolean() && node.asBoolean();
    }
}
