package com.abhout.pocket_ledger_be.setting;

import java.util.List;

public record DriveSyncMeta(
    String lastSyncedAt, String status,
    int imported, int duplicates, int filesStored, int
    review,
    List<String> errors
) {}
