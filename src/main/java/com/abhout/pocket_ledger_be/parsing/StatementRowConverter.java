package com.abhout.pocket_ledger_be.parsing;

import com.abhout.pocket_ledger_be.parsing.models.StatementConversionResult;
import com.abhout.pocket_ledger_be.parsing.models.TransactionCandidate;
import com.abhout.pocket_ledger_be.parsing.parsers.AmountParser;
import com.abhout.pocket_ledger_be.parsing.parsers.DateParser;
import com.abhout.pocket_ledger_be.transaction.models.TransactionValidator;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@NoArgsConstructor
public final class StatementRowConverter {
    public  static StatementConversionResult convert(
            List<List<String>> dataRows,
            Map<Integer, ColumnRole> colMapping,
            List<String> knownCategories,
            String fallbackAccount,
            boolean dayFirst
    ) {
        int dateIdx = indexOf(colMapping, ColumnRole.DATE);
        int merchantIdx = indexOf(colMapping, ColumnRole.MERCHANT);
        int amountIdx = indexOf(colMapping, ColumnRole.AMOUNT);
        int debitIdx = indexOf(colMapping, ColumnRole.DEBIT);
        int creditIdx = indexOf(colMapping, ColumnRole.CREDIT);
        int categoryIdx = indexOf(colMapping, ColumnRole.CATEGORY);
        int accountIdx = indexOf(colMapping, ColumnRole.ACCOUNT);

        Map<String, String> categoryLookup = knownCategories.stream()
                .collect(Collectors.toMap(
                        String::toLowerCase,
                        s -> s,
                        (a, b) -> a
                    )
                );
        List<TransactionCandidate> transactionCandidates = new ArrayList<>();
        int unparseable = 0;
        for (List<String> row : dataRows) {
            LocalDate date = dateIdx >= 0 ? DateParser.parse(cell(row, dateIdx), dayFirst) : null;
            String merchant = merchantIdx >= 0 ? cell(row, merchantIdx).trim() : "";

            BigDecimal amount = null;
            String type = "expense";

            BigDecimal debit = debitIdx >= 0 ? AmountParser.parse(cell(row, debitIdx)) : null;
            BigDecimal credit = creditIdx >= 0 ? AmountParser.parse(cell(row, creditIdx)) : null;

            if (debit != null && debit.signum() != 0) {
                amount = debit.abs();
                type = "expense";
            } else if (credit != null && credit.signum() != 0) {
                amount = credit.abs();
                type = "income";
            } else if (amountIdx >= 0) {
                BigDecimal parsed = AmountParser.parse(cell(row, amountIdx));
                if (parsed != null && parsed.signum() != 0) {
                    amount = parsed.abs();
                    type = parsed.signum() < 0 ? "expense" : "income";
                }
            }

            if (date == null || merchant.isEmpty() || amount== null || amount.signum() <= 0) {
                unparseable++;
                continue;
            }

            String rawCategory = categoryIdx >= 0 ? cell(row,categoryIdx).trim() : "";
            String category =categoryLookup.getOrDefault(rawCategory.toLowerCase(), TransactionValidator.DEFAULT_CATEGORY);

            String rawAccount = accountIdx >= 0 ? cell(row,accountIdx).trim() : "";
            String account = rawAccount.isEmpty() ?fallbackAccount : rawAccount;

            transactionCandidates.add(new TransactionCandidate
                    (
                        date,
                        merchant.length() > 200 ? merchant.substring(0, 200) : merchant,
                        amount, type, category, account
                    )
            );
        }

        return new StatementConversionResult(transactionCandidates,unparseable);
    }

    private static String cell(List<String> row, int idx) {
        return idx < row.size() ? row.get(idx) : "";
    }

    private static int indexOf(Map<Integer, ColumnRole>
                                       mapping, ColumnRole role) {
        return mapping.entrySet().stream()
                .filter(e -> e.getValue() == role)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
    }
}
