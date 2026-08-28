package com.abhout.pocket_ledger_be.sync;

import com.abhout.pocket_ledger_be.drive.DriveClient;
import com.abhout.pocket_ledger_be.drive.DriveFile;
import com.abhout.pocket_ledger_be.setting.DriveFolder;
import com.abhout.pocket_ledger_be.setting.DriveSyncMeta;
import com.abhout.pocket_ledger_be.setting.SettingService;
import com.abhout.pocket_ledger_be.setting.SettingsResponse;
import com.abhout.pocket_ledger_be.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriveSyncRunnerTest {

    @Mock private SettingService settingService;
    @Mock private DriveClient driveClient;
    @Mock private DriveSyncService driveSyncService;

    private DriveSyncRunner driveSyncRunner;
    private final User user = new User("sync-runner-test@example.com", "irrelevant-hash");

    @BeforeEach
    void setUp() {
        driveSyncRunner = new DriveSyncRunner(settingService, driveClient, driveSyncService);
    }

    private SettingsResponse settingsWithDrive(String driveFolderId, String driveResetAt) {
        DriveFolder folder = driveFolderId == null ? null
                : new DriveFolder(driveFolderId, "Inbox", "https://drive.google.com/drive/folders/" + driveFolderId);
        return new SettingsResponse(
                List.of("Groceries", "Dining"),
                List.of(),
                List.of(), List.of(), List.of(), List.of(),
                List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO,
                false,
                "all-time",
                folder,
                null,
                null,
                driveResetAt,
                true
        );
    }

    private DriveFile file(String id) {
        return new DriveFile(id, id + ".csv", "text/csv", Instant.now());
    }

    @Test
    void notConfigured_returnsEmptyAndTouchesNothingElse() {
        when(settingService.decode(user)).thenReturn(settingsWithDrive(null, null));

        Optional<DriveSyncMeta> result = driveSyncRunner.runSyncForUser(user);

        assertTrue(result.isEmpty());
        verifyNoInteractions(driveClient);
        verifyNoInteractions(driveSyncService);
        verify(settingService, never()).recordDriveSyncResult(any(), any(), any(), anyBoolean());
    }

    @Test
    void happyPath_filtersAlreadyProcessedFilesAndAggregatesOutcomes() throws IOException {
        when(settingService.decode(user)).thenReturn(settingsWithDrive("folder-1", null));
        when(settingService.getProcessedFileIds(user)).thenReturn(List.of("old-file"));
        when(driveClient.listFiles("folder-1", null)).thenReturn(List.of(
                file("old-file"), file("new-file-1"), file("new-file-2")));

        DriveFileOutcome outcome1 = new DriveFileOutcome("new-file-1", true, 2, 1, false, null);
        DriveFileOutcome outcome2 = new DriveFileOutcome("new-file-2", true, 0, 0, true, null);
        when(driveSyncService.processFile(eq(user), argThat(f -> f.id().equals("new-file-1")), any(), eq(true)))
                .thenReturn(outcome1);
        when(driveSyncService.processFile(eq(user), argThat(f -> f.id().equals("new-file-2")), any(), eq(true)))
                .thenReturn(outcome2);

        Optional<DriveSyncMeta> result = driveSyncRunner.runSyncForUser(user);

        verify(driveSyncService, never()).processFile(eq(user), argThat(f -> f.id().equals("old-file")), any(), anyBoolean());
        verify(driveSyncService, times(2)).processFile(eq(user), any(), any(), eq(true));

        ArgumentCaptor<List<String>> processedCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<DriveSyncMeta> metaCaptor = ArgumentCaptor.forClass(DriveSyncMeta.class);
        verify(settingService).recordDriveSyncResult(eq(user), processedCaptor.capture(), metaCaptor.capture(), eq(true));

        List<String> updatedProcessed = processedCaptor.getValue();
        assertEquals(3, updatedProcessed.size());
        assertTrue(updatedProcessed.containsAll(List.of("old-file", "new-file-1", "new-file-2")));

        DriveSyncMeta meta = metaCaptor.getValue();
        assertEquals(2, meta.imported());
        assertEquals(1, meta.duplicates());
        assertEquals(2, meta.filesStored());
        assertEquals(1, meta.review());
        assertEquals("complete", meta.status());
        assertTrue(meta.errors().isEmpty());

        assertTrue(result.isPresent());
        assertEquals(meta, result.get());
    }

    @Test
    void driveUnreachable_recordsFailureWithoutTouchingProcessedList() throws IOException {
        when(settingService.decode(user)).thenReturn(settingsWithDrive("folder-1", null));
        when(settingService.getProcessedFileIds(user)).thenReturn(List.of("a", "b"));
        when(driveClient.listFiles(eq("folder-1"), any())).thenThrow(new IOException("network blip"));

        Optional<DriveSyncMeta> result = driveSyncRunner.runSyncForUser(user);

        verifyNoInteractions(driveSyncService);

        ArgumentCaptor<List<String>> processedCaptor = ArgumentCaptor.forClass(List.class);
        verify(settingService).recordDriveSyncResult(eq(user), processedCaptor.capture(), any(), eq(false));
        assertEquals(List.of("a", "b"), processedCaptor.getValue());

        assertTrue(result.isPresent());
        assertEquals("partial", result.get().status());
        assertFalse(result.get().errors().isEmpty());
    }

    @Test
    void resetAtPresent_isParsedAndPassedToDriveClient() throws IOException {
        when(settingService.decode(user)).thenReturn(settingsWithDrive("folder-1", "2026-08-01T00:00:00Z"));
        when(settingService.getProcessedFileIds(user)).thenReturn(List.of());
        when(driveClient.listFiles(any(), any())).thenReturn(List.of());

        driveSyncRunner.runSyncForUser(user);

        verify(driveClient).listFiles("folder-1", Instant.parse("2026-08-01T00:00:00Z"));
    }

    @Test
    void resetAtAbsent_passesNullModifiedAfter() throws IOException {
        when(settingService.decode(user)).thenReturn(settingsWithDrive("folder-1", null));
        when(settingService.getProcessedFileIds(user)).thenReturn(List.of());
        when(driveClient.listFiles(any(), any())).thenReturn(List.of());

        driveSyncRunner.runSyncForUser(user);

        verify(driveClient).listFiles("folder-1", null);
    }

    @Test
    void errorsAreCappedAtTenEvenWhenMoreFilesFail() throws IOException {
        when(settingService.decode(user)).thenReturn(settingsWithDrive("folder-1", null));
        when(settingService.getProcessedFileIds(user)).thenReturn(List.of());

        List<DriveFile> twelveFiles = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            String id = "file-" + i;
            twelveFiles.add(file(id));
            when(driveSyncService.processFile(eq(user), argThat(f -> f.id().equals(id)), any(), eq(true)))
                    .thenReturn(new DriveFileOutcome(id, false, 0, 0, false, id + " failed"));
        }
        when(driveClient.listFiles(any(), any())).thenReturn(twelveFiles);

        driveSyncRunner.runSyncForUser(user);

        ArgumentCaptor<DriveSyncMeta> metaCaptor = ArgumentCaptor.forClass(DriveSyncMeta.class);
        verify(settingService).recordDriveSyncResult(eq(user), any(), metaCaptor.capture(), eq(false));
        assertEquals(10, metaCaptor.getValue().errors().size());
    }

    @Test
    void fileStoredWithZeroTransactions_stillClearsFreshStart() throws IOException {
        when(settingService.decode(user)).thenReturn(settingsWithDrive("folder-1", null));
        when(settingService.getProcessedFileIds(user)).thenReturn(List.of());
        when(driveClient.listFiles(any(), any())).thenReturn(List.of(file("review-file")));
        when(driveSyncService.processFile(eq(user), any(), any(), eq(true)))
                .thenReturn(new DriveFileOutcome("review-file", true, 0, 0, true, null));

        driveSyncRunner.runSyncForUser(user);
        verify(settingService).recordDriveSyncResult(eq(user), any(), any(), eq(true));
    }
}
