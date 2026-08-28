package com.abhout.pocket_ledger_be.sync;

import com.abhout.pocket_ledger_be.document.DocumentService;
import com.abhout.pocket_ledger_be.document.enums.DocumentSource;
import com.abhout.pocket_ledger_be.document.enums.DocumentStatus;
import com.abhout.pocket_ledger_be.drive.DriveClient;
import com.abhout.pocket_ledger_be.drive.DriveFile;
import com.abhout.pocket_ledger_be.extraction.ReceiptExtraction;
import com.abhout.pocket_ledger_be.extraction.ReceiptExtractionRefusedException;
import com.abhout.pocket_ledger_be.extraction.ReceiptExtractionService;
import com.abhout.pocket_ledger_be.parsing.StatementParsingService;
import com.abhout.pocket_ledger_be.parsing.models.StatementParseResult;
import com.abhout.pocket_ledger_be.transaction.DTOs.TransactionWriteResponse;
import com.abhout.pocket_ledger_be.transaction.TransactionService;
import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static com.abhout.pocket_ledger_be.document.enums.DocumentStatus.isTabular;

@Service
@RequiredArgsConstructor
public class DriveSyncService {
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final String DRIVE_TAG = "Drive import";
    private static final String DRIVE_ACCOUNT = "Drive import";

    private final DriveClient driveClient;
    private final StatementParsingService statementParsingService;
    private final ReceiptExtractionService receiptExtractionService;
    private final DocumentService documentService;
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    public  DriveFileOutcome processFile(
            User user,
            DriveFile file,
            List<String> knownCategories,
            boolean dayFirst
    ){
        byte[] content;
        try {
            content = driveClient.downloadFile(file.id());
        } catch (IOException e) {
            return DriveFileOutcome.failed(
                    file.name() + " could not be downloaded",
                    file.id()
            );
        }

        if(content.length > MAX_FILE_BYTES){
            return DriveFileOutcome.failed(
                    file.name() + " is too large. Max size is 20 MB.",
                    file.id()
            );
        }

        try {
            return isTabular(
                    file.mimeType(),
                    file.name()
            ) ?
                    processTabular(
                            user,
                            file,
                            content,
                            knownCategories,
                            dayFirst
                    ):
                    processReceipt(
                            user,
                            file,
                            content,
                            knownCategories
                    );
        } catch (RuntimeException e){
            return DriveFileOutcome.failed(
                    file.name() + " could not be processed.",
                    file.id()
            );
        }
    }

    DriveFileOutcome processTabular(
            User user,
            DriveFile file,
            byte[] content,
            List<String> knownCategories,
            boolean dayFirst
    ) {
        StatementParseResult res = statementParsingService.parse(
                content,
                file.mimeType(),
                knownCategories,
                DRIVE_ACCOUNT,
                dayFirst
        );
        boolean grounded = res.mappingComplete()
                && !res.transactions().isEmpty();

        documentService.storeDocument(
                user,
                file.name(),
                content,
                file.mimeType(),
                grounded ? DocumentStatus.STORED : DocumentStatus.REVIEW,
                DocumentSource.GOOGLE_DRIVE
        );

        if (!grounded){
            return new DriveFileOutcome(
                    file.id(),
                    true,
                    0,
                     0,
                    true,
                    null
            );
        }

        List<JsonNode> inputs = res.transactions().stream()
                .map(c -> toJsonNode(
                        c.date().toString(),
                        c.merchant(),
                        c.amount(),
                        c.type(),
                        c.category(),
                        c.account(),
                        false
                )).toList();

        TransactionWriteResponse res2 = transactionService.insertTransactions(
                user,
                inputs,
                true
        );

        return  new DriveFileOutcome(
                file.id(),
                true,
                res2.inserted(),
                res2.duplicates(),
                res2.needsReview() > 0,
                null
        );
    }

    private DriveFileOutcome processReceipt(
            User user,
            DriveFile file,
            byte[] content,
            List<String> knownCategories
    ){
        ReceiptExtraction extraction;
        try {
            extraction = receiptExtractionService.extract(
                    content,
                    file.mimeType(),
                    knownCategories
            );
        } catch (ReceiptExtractionRefusedException e){
            documentService.storeDocument(
                    user,
                    file.name(),
                    content,
                    file.mimeType(),
                    DocumentStatus.REVIEW,
                    DocumentSource.GOOGLE_DRIVE
            );
            return new DriveFileOutcome(
                    file.id(),
                    true,
                    0,
                    0,
                    true,
                    file.name() + ": extraction declined."
            );
        }

        boolean grounded = extraction.date() != null
                && extraction.merchant() != null
                && extraction.amount() != null
                && extraction.type() != null;

        documentService.storeDocument(
                user,
                file.name(),
                content,
                file.mimeType(),
                grounded ? DocumentStatus.STORED : DocumentStatus.REVIEW,
                DocumentSource.GOOGLE_DRIVE
        );
        if (!grounded){
            return new DriveFileOutcome(
                    file.id(),
                    true,
                    0,
                    0,
                    true,
                    null
            );
        }

        JsonNode input = toJsonNode(
                extraction.date(),
                extraction.merchant(),
                extraction.amount(),
                extraction.type(),
                extraction.category(),
                DRIVE_ACCOUNT,
                true
        );

        TransactionWriteResponse res = transactionService.insertTransactions(
                user,
                List.of(input),
                true
        );

        return new DriveFileOutcome(
                file.id(),
                true,
                res.inserted(),
                res.duplicates(),
                res.needsReview() > 0,
                null
        );
    }

    private JsonNode toJsonNode(
            String date,
            String merchant,
            BigDecimal amount,
            String type,
            String category,
            String account,
            boolean receipt
    ) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("date", date);
        node.put("merchant", merchant);
        node.put("amount", amount);
        node.put("type", type);
        if (category != null) node.put("category", category);
        node.put("account", account);
        node.put("receipt", receipt);
        node.put("source", "google-drive");
        node.putArray("tags").add(DRIVE_TAG);
        return node;
    }
}
