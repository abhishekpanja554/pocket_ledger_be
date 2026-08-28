package com.abhout.pocket_ledger_be.sync;

import com.abhout.pocket_ledger_be.document.DocumentService;
import com.abhout.pocket_ledger_be.document.enums.DocumentSource;
import com.abhout.pocket_ledger_be.document.enums.DocumentStatus;
import com.abhout.pocket_ledger_be.drive.DriveClient;
import com.abhout.pocket_ledger_be.drive.DriveFile;
import com.abhout.pocket_ledger_be.extraction.ReceiptExtraction;
import com.abhout.pocket_ledger_be.extraction.ReceiptExtractionRefusedException;
import com.abhout.pocket_ledger_be.extraction.ReceiptExtractor;
import com.abhout.pocket_ledger_be.parsing.StatementParsingService;
import com.abhout.pocket_ledger_be.parsing.models.StatementParseResult;
import com.abhout.pocket_ledger_be.parsing.models.TransactionCandidate;
import com.abhout.pocket_ledger_be.transaction.DTOs.TransactionWriteResponse;
import com.abhout.pocket_ledger_be.transaction.TransactionService;
import com.abhout.pocket_ledger_be.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DriveSyncServiceTest {

    @Mock
    private DriveClient driveClient;
    @Mock
    private StatementParsingService statementParsingService;
    @Mock
    private ReceiptExtractor receiptExtractionService;
    @Mock
    private DocumentService documentService;
    @Mock
    private TransactionService transactionService;

    private DriveSyncService driveSyncService;
    private final User user = new User(
            "sync-test@email.com",
            "test-hash");

    @BeforeEach
    void setUp() {
        driveSyncService = new DriveSyncService(
                driveClient,
                statementParsingService,
                receiptExtractionService,
                documentService,
                transactionService,
                new ObjectMapper()
        );
    }

    private DriveFile csvFile(){
        return  new DriveFile(
                "file-1",
                "statement.csv",
                "text/csv",
                Instant.now()
        );
    }

    private DriveFile imageFile() {
        return new DriveFile(
                "file-2",
                "receipt.jpg",
                "image/jpeg",
                Instant.now()
        );
    }

    @Test
    void tabularHappyPath_storesAsStoredAndInsertsTransactions() throws IOException {
        DriveFile csvFile = csvFile();
        when(driveClient.downloadFile("file-1")).thenReturn("csv bytes".getBytes());

        TransactionCandidate candidate = new TransactionCandidate(
                LocalDate.of(2026, 6, 1),
                "ATM Withdrawal",
                new BigDecimal("500.00"),
                "expense",
                "Groceries",
                "Drive import"
        );

        when(statementParsingService.parse(any(),eq("text/csv"), any(), eq("Drive import"), anyBoolean()))
                .thenReturn( new StatementParseResult(List.of(candidate), 0, true));
        when(transactionService.insertTransactions(eq(user), anyList(), eq(true)))
                .thenReturn(new TransactionWriteResponse(
                        1,
                        0,
                        0,
                        0, List.of(),
                        List.of()
                )
        );
        DriveFileOutcome outcome = driveSyncService.processFile(
                user,
                csvFile,
                List.of("Groceries"), true
        );

        verify(documentService).storeDocument(eq(user),
                eq("statement.csv"), any(), eq("text/csv"),
                eq(DocumentStatus.STORED),
                eq(DocumentSource.GOOGLE_DRIVE)
        );

        ArgumentCaptor<List<JsonNode>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionService).insertTransactions(eq(user), captor.capture(), eq(true));

        JsonNode sent = captor.getValue().getFirst();
        assertEquals("google-drive", sent.get("source").asString());
        assertEquals("Drive import", sent.get("tags").get(0).asString());
        assertFalse(sent.get("receipt").asBoolean());
        assertTrue(outcome.stored());
        assertEquals(1, outcome.transactionsImported());
        assertFalse(outcome.needsReview());
        assertNull(outcome.error());
    }

    @Test
    void tabularIncompleteMapping_storesAsReviewAndNeverInsertsTransactions() throws IOException {
        DriveFile csvFile = csvFile();
        when(driveClient.downloadFile("file-1")).thenReturn("junk".getBytes());
        when(statementParsingService.parse(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new StatementParseResult(List.of(), 0, false));

        DriveFileOutcome outcome = driveSyncService.processFile(user, csvFile, List.of(), true);

        verify(documentService).storeDocument(any(), any(), any(), any(),
                eq(DocumentStatus.REVIEW), eq(DocumentSource.GOOGLE_DRIVE));
        verifyNoInteractions(transactionService);
        assertTrue(outcome.stored());
        assertTrue(outcome.needsReview());
        assertEquals(0, outcome.transactionsImported());
    }

    @Test
    void tabularMappingCompleteButZeroRowsExtracted_stillStoresAsReview() throws IOException {
        // The exact bug caught while writing DriveSyncService: mappingComplete=true alone must NOT
        // be enough to mark the document STORED if nothing was actually extracted from it.
        DriveFile csvFile = csvFile();
        when(driveClient.downloadFile("file-1")).thenReturn("header only, no data rows".getBytes());
        when(statementParsingService.parse(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new StatementParseResult(List.of(), 0, true)); // mappingComplete=true, but empty

        DriveFileOutcome outcome = driveSyncService.processFile(user, csvFile, List.of(), true);

        verify(documentService).storeDocument(any(), any(), any(), any(),
                eq(DocumentStatus.REVIEW), any());
        verifyNoInteractions(transactionService);
        assertTrue(outcome.needsReview());
    }

    @Test
    void receiptHappyPath_groundedExtraction_storesAsStoredAndInsertsOneTransaction() throws IOException {
        DriveFile imageFile = imageFile();
        when(driveClient.downloadFile("file-2")).thenReturn("jpeg bytes".getBytes());

        ReceiptExtraction extraction = new ReceiptExtraction(
                "2026-06-01", "Swiggy", new BigDecimal("450.00"), "expense", "Dining");
        when(receiptExtractionService.extract(any(), eq("image/jpeg"), any())).thenReturn(extraction);
        when(transactionService.insertTransactions(eq(user), anyList(), eq(true)))
                .thenReturn(new TransactionWriteResponse(1, 0, 0, 0, List.of(), List.of()));

        DriveFileOutcome outcome = driveSyncService.processFile(user, imageFile, List.of("Dining"), true);

        verify(documentService).storeDocument(any(), any(), any(), any(),
                eq(DocumentStatus.STORED), eq(DocumentSource.GOOGLE_DRIVE));

        ArgumentCaptor<List<JsonNode>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionService).insertTransactions(eq(user), captor.capture(), eq(true));
        assertTrue(captor.getValue().getFirst().get("receipt").asBoolean());

        assertEquals(1, outcome.transactionsImported());
        assertFalse(outcome.needsReview());
    }

    @Test
    void receiptUngroundedExtraction_storesAsReviewAndNeverInsertsTransaction() throws IOException {
        DriveFile imageFile = imageFile();
        when(driveClient.downloadFile("file-2")).thenReturn("blurry jpeg".getBytes());

        // amount couldn't be read — one null field is enough to make this ungrounded
        ReceiptExtraction extraction = new ReceiptExtraction("2026-06-01", "Swiggy", null, "expense", null);
        when(receiptExtractionService.extract(any(), any(), any())).thenReturn(extraction);

        DriveFileOutcome outcome = driveSyncService.processFile(user, imageFile, List.of(), true);

        verify(documentService).storeDocument(any(), any(), any(), any(),
                eq(DocumentStatus.REVIEW), any());
        verifyNoInteractions(transactionService);
        assertTrue(outcome.needsReview());
        assertEquals(0, outcome.transactionsImported());
    }

    @Test
    void receiptExtractionRefused_stillStoresDocumentAndStillMarksProcessed() throws IOException {
        DriveFile imageFile = imageFile();
        when(driveClient.downloadFile("file-2")).thenReturn("bytes".getBytes());
        when(receiptExtractionService.extract(any(), any(), any()))
                .thenThrow(new ReceiptExtractionRefusedException());

        DriveFileOutcome outcome = driveSyncService.processFile(user, imageFile, List.of(), true);

        verify(documentService).storeDocument(any(), any(), any(), any(),
                eq(DocumentStatus.REVIEW), eq(DocumentSource.GOOGLE_DRIVE));
        verifyNoInteractions(transactionService);
        // A refusal isn't transient — retrying tomorrow won't change it, so it's still marked processed.
        assertTrue(outcome.stored());
        assertNotNull(outcome.error());
    }

    @Test
    void downloadFailure_neverStoresDocumentAndIsNotMarkedProcessed() throws IOException {
        DriveFile csvFile = csvFile();
        when(driveClient.downloadFile("file-1")).thenThrow(new IOException("network blip"));

        DriveFileOutcome outcome = driveSyncService.processFile(user, csvFile, List.of(), true);

        verifyNoInteractions(documentService);
        verifyNoInteractions(transactionService);
        assertFalse(outcome.stored()); // must retry tomorrow
        assertNotNull(outcome.error());
    }

    @Test
    void oversizedFile_rejectedBeforeAnyProcessing() throws IOException {
        DriveFile csvFile = csvFile();
        byte[] tooLarge = new byte[21 * 1024 * 1024]; // over the 20MB cap
        when(driveClient.downloadFile("file-1")).thenReturn(tooLarge);

        DriveFileOutcome outcome = driveSyncService.processFile(user, csvFile, List.of(), true);

        verifyNoInteractions(statementParsingService);
        verifyNoInteractions(documentService);
        assertFalse(outcome.stored());
        assertTrue(outcome.error().contains("20 MB"));
    }

    @Test
    void unexpectedExceptionDuringParsing_caughtByOuterSafetyNet() throws IOException {
        DriveFile csvFile = csvFile();
        when(driveClient.downloadFile("file-1")).thenReturn("bytes".getBytes());
        when(statementParsingService.parse(any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new RuntimeException("unexpected bug"));

        DriveFileOutcome outcome = driveSyncService.processFile(user, csvFile, List.of(), true);

        assertFalse(outcome.stored());
        assertNotNull(outcome.error());
    }
}
