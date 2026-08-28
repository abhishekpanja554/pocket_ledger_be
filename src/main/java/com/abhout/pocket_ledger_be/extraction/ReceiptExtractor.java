package com.abhout.pocket_ledger_be.extraction;

import java.util.List;

public interface ReceiptExtractor {
    ReceiptExtraction extract(
            byte[] content,
            String mimeType,
            List<String> knownCategories
    );
}
