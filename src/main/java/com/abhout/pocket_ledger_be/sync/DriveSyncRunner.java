package com.abhout.pocket_ledger_be.sync;

import com.abhout.pocket_ledger_be.drive.DriveClient;
import com.abhout.pocket_ledger_be.drive.DriveFile;
import com.abhout.pocket_ledger_be.setting.DriveSyncMeta;
import com.abhout.pocket_ledger_be.setting.SettingService;
import com.abhout.pocket_ledger_be.setting.SettingsResponse;
import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DriveSyncRunner {
    private static final int MAX_ERRORS_RECORDED = 10;
    private final SettingService settingService;
    private final DriveClient driveClient;
    private final DriveSyncService driveSyncService;

    public Optional<DriveSyncMeta> runSyncForUser(User user){
        SettingsResponse response = settingService.decode(user);
        if(response.driveFolder() == null){
            return Optional.empty();
        }

        List<String> processed = settingService.getProcessedFileIds(user);
        Instant resetAt = response.driveResetAt() != null ?
                Instant.parse(response.driveResetAt()) : null;

        List<DriveFile> files;
        try {
            files = driveClient.listFiles(response.driveFolder().id(), resetAt);
        } catch (IOException e){
            DriveSyncMeta failedMeta = new DriveSyncMeta(
                    Instant.now().toString(),
                    "partial",
                    0,
                    0,
                    0,
                    0,
                    List.of("Could not reach Google Drive.")
            );
            settingService.recordDriveSyncResult(
                    user,
                    processed,
                    failedMeta,
                    false
            );
            return Optional.of(failedMeta);
        }

        Set<String> processedSet = new HashSet<>(processed);
        List<DriveFile> newFiles = files.stream()
                .filter(f -> !processedSet.contains(f.id())
                ).toList();

        int imported = 0, duplicates = 0, filesStored = 0, review = 0;
        List<String> errors = new ArrayList<>();
        List<String> newlyProcessed = new ArrayList<>();
        for (DriveFile file : newFiles){
            DriveFileOutcome output = driveSyncService.processFile(
                    user,
                    file,
                    response.categories(),
                    true
            );
            imported += output.transactionsImported();
            duplicates += output.duplicates();
            if (output.stored()){
                filesStored++;
                newlyProcessed.add(file.id());
            }
            if (output.needsReview()){
                review++;
            }
            if (output.error() != null && errors.size() < MAX_ERRORS_RECORDED){
                errors.add(output.error());
            }
        }

        List<String> updatedProcessed = new ArrayList<>(processed);
        updatedProcessed.addAll(newlyProcessed);
        DriveSyncMeta meta = new DriveSyncMeta(
                Instant.now().toString(),
                errors.isEmpty() ? "complete" : "partial",
                imported,
                duplicates,
                filesStored,
                review,
                errors
        );
        settingService.recordDriveSyncResult
                (
                        user,
                        updatedProcessed,
                        meta,
                        imported > 0 || filesStored > 0
                );
        return Optional.of(meta);
    }
}
