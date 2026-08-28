package com.abhout.pocket_ledger_be.sync;

public record DriveFileOutcome(
        String driveFileId,
        boolean stored,
        int transactionsImported,
        int duplicates,
        boolean needsReview,
        String error
) {
    public  static DriveFileOutcome failed(
            String error,
            String driveFileId
    ) {
        return new DriveFileOutcome(
                driveFileId,
                false,
                0,
                0,
                false,
                error);
    }
}
