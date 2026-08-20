package com.abhout.pocket_ledger_be.setting;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

public record SettingsResponse(
        List<String> categories,
        List<String> accounts,
        List<JsonNode> goals,
        List<JsonNode> budgets,
        List<JsonNode> subscriptions,
        List<JsonNode> recurring,
        List<String> dismissedPatterns,
        BigDecimal assets,
        BigDecimal liabilities,
        boolean netWorthConfigured,
        String selectedPeriod,
        DriveFolder driveFolder,
        DriveSchedule driveSchedule,
        DriveSyncMeta driveSync,
        String driveResetAt,
        boolean freshStart
) {
}
