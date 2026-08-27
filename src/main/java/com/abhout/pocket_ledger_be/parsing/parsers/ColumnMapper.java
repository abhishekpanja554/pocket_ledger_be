package com.abhout.pocket_ledger_be.parsing.parsers;

import com.abhout.pocket_ledger_be.parsing.ColumnRole;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor
public final class ColumnMapper {
    public static String normalizeHeader(String header) {
        return header.toLowerCase()
                .replaceAll("[().,/]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static boolean hasWord(String header, String word) {
        return Pattern.compile("(^| )" + word + "( |$)").matcher(header).find();
    }

    public static ColumnRole roleForHeader(String header) {
        String h = normalizeHeader(header);
        if (h.isEmpty()) return ColumnRole.IGNORE;

        if (h.contains("balance") || hasWord(h, "bal"))
            return ColumnRole.IGNORE;
        if (h.contains("ref") || h.contains("chq") || h.contains("cheque"))
            return ColumnRole.IGNORE;

        if (h.contains("withdrawal") || h.contains("debit")
                || h.contains("paid out")
                || h.contains("money out") || hasWord(h,
                "dr")
        )   return ColumnRole.DEBIT;

        if (h.contains("deposit") || h.contains("credit")
                || h.contains("paid in")
                || h.contains("money in")
                || hasWord(h, "cr")
        )   return ColumnRole.CREDIT;

        if (h.contains("category")
                || h.contains("classification")
        )   return ColumnRole.CATEGORY;
        if (h.contains("account")
                || hasWord(h, "card")
                || hasWord(h, "source")
        )   return ColumnRole.ACCOUNT;
        if (h.contains("date")
                || hasWord(h, "dt")
        )   return ColumnRole.DATE;

        if (h.contains("narration") ||
                h.contains("particulars")
                || h.contains("description")
                || h.contains("remarks")
                || h.contains("narrative")
                || h.contains("details")
                || h.contains("merchant")
                || h.contains("payee")
                || h.contains("memo")
                || hasWord(h, "name")
        )   return ColumnRole.MERCHANT;

        if (h.contains("amount")
                || hasWord(h, "amt")
                || hasWord(h, "value")
        )   return ColumnRole.AMOUNT;

        return ColumnRole.IGNORE;
    }

    public static Map<Integer, ColumnRole> guessMapping(List<String> headers)
    {
        Map<Integer, ColumnRole> mapping = new LinkedHashMap<>();
        Set<ColumnRole> used = new HashSet<>();
        for (int i = 0; i < headers.size(); i++) {
            ColumnRole role = roleForHeader(headers.get(i));
            if (role != ColumnRole.IGNORE && used.add(role)) {
                mapping.put(i, role);
            } else {
                mapping.put(i, ColumnRole.IGNORE);
            }
        }
        return mapping;
    }

    public static boolean mappingIsComplete(Map<Integer, ColumnRole> mapping) {
        Collection<ColumnRole> roles = mapping.values();
        boolean hasDate = roles.contains(ColumnRole.DATE);
        boolean hasMerchant = roles.contains(ColumnRole.MERCHANT);
        boolean hasValue = roles.contains(ColumnRole.AMOUNT)
                || roles.contains(ColumnRole.DEBIT)
                || roles.contains(ColumnRole.CREDIT);
        return hasDate && hasMerchant && hasValue;
    }

    /**
     * Bank statements open with the account holder's name,
     statement period, blank
     * lines before the real header row — scores the first
     several rows by how many
     * columns they identify and takes the best one, rather
     than assuming row 0.
     */

    public static int findHeaderRow(List<List<String>> rows, int searchDepth)
    {
        int bestIndex = -1;
        int bestScore = 0;
        int limit = Math.min(searchDepth, rows.size());

        for (int i = 0; i < limit; i++) {
            Map<Integer, ColumnRole> mapping = guessMapping(rows.get(i));
            Set<ColumnRole> roles = mapping.values().stream()
                    .filter(r -> r != ColumnRole.IGNORE)
                    .collect(Collectors.toSet());
            boolean hasDate = roles.contains(ColumnRole.DATE);
            boolean hasMerchant = roles.contains(ColumnRole.MERCHANT);
            boolean hasValue = roles.contains(ColumnRole.AMOUNT)
                    || roles.contains(ColumnRole.DEBIT)
                    || roles.contains(ColumnRole.CREDIT);
            if (!(hasDate && hasMerchant && hasValue))
                continue;

            int score = roles.size();
            if (score > bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex; // -1 = no recognizable header found in the search window
    }
}
