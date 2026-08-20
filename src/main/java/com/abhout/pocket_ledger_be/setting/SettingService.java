package com.abhout.pocket_ledger_be.setting;

import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.StringNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettingService {
    private final SettingRepository settingRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.default-timezone}")
    private String defaultTimeZone;

    private static final List<String> STARTER_CATEGORIES =
        List.of(
            "Housing", "Groceries", "Shopping", "Dining",
            "Transportation",
            "Utilities", "Subscriptions", "Insurance",
            "Health",
            "Entertainment", "Income", "Needs review",
            "Other");
    private static final List<String> STARTER_ACCOUNTS =
        List.of("Main bank Account", "Credit Card", "Cash");
    private static final Set<String> PERIOD_IDS = Set.of(
        "all-time", "this-month", "last-month",
        "last-3-months", "last-6-months", "this-year");

    public SettingsResponse decode(User user) {
        Map<String, JsonNode> raw =
                settingRepository.findByUserId(user.getId()).stream()
                        .collect(Collectors.toMap(Setting::getKey,
                                this::parseValue));

        return new SettingsResponse(
            asStringList(raw.get("categories"), STARTER_CATEGORIES),
            asStringList(raw.get("accounts"), STARTER_ACCOUNTS),
            asNodeList(raw.get("goals")),
            asNodeList(raw.get("budgets")),
            asNodeList(raw.get("subscriptions")),
            asNodeList(raw.get("recurring")),
            asStringList(raw.get("dismissedPatterns"), List.of()),
            asNumber(raw.get("assets"), BigDecimal.ZERO),
            asNumber(raw.get("liabilities"), BigDecimal.ZERO),
            asStrictBoolean(raw.get("netWorthConfigured")),
            asPeriod(raw.get("selectedPeriod")),
            asDriveFolder(raw.get("driveFolder")),
            asDriveSchedule(raw.get("driveSchedule")),
            asDriveSync(raw.get("driveSync")),
            asNullableText(raw.get("driveResetAt")),
            asStrictBoolean(raw.get("freshStart"))
        );
    }

    private JsonNode parseValue(Setting row) {
        try {
            return objectMapper.readTree(row.getValue());
        } catch (JacksonException e) {
            return StringNode.valueOf(row.getValue());
        }
    }

    private List<String> asStringList(JsonNode node, List<String> fallback) {
        if (node == null || !node.isArray()) return fallback;
        return node.valueStream().map(n -> n.asString("")).toList();
    }

    private List<JsonNode> asNodeList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        return node.valueStream().toList();
    }

    private BigDecimal asNumber(JsonNode node, BigDecimal fallback) {
        if (node == null) return fallback;
        if (node.isNumber()) return node.decimalValue();
        if (node.isString()) {
            try {
                return new BigDecimal(node.asString(""));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private boolean asStrictBoolean(JsonNode node) {
        return node != null && node.isBoolean() && node.asBoolean();
    }

    private String asPeriod(JsonNode node) {
        if (node != null && node.isString() && PERIOD_IDS.contains(node.asString(""))) {
            return node.asString("");
        }
        return "all-time";
    }

    private String asNullableText(JsonNode node) {
        return node != null && node.isString() ? node.asString("") : null;
    }

    private DriveFolder asDriveFolder(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            return objectMapper.treeToValue(node, DriveFolder.class);
        } catch (JacksonException e) {
            return null;
        }
    }

    private DriveSchedule asDriveSchedule(JsonNode node) {
        JsonNode time = field(node, "time");
        JsonNode timezone = field(node, "timezone");
        return new DriveSchedule(
            time != null && time.isString() ? time.asString("08:00") : "08:00",
            timezone != null && timezone.isString() ? timezone.asString(defaultTimeZone) : defaultTimeZone,
            "daily"
        );
    }

    private DriveSyncMeta asDriveSync(JsonNode node) {
        return new DriveSyncMeta(
            asNullableText(field(node, "lastSyncedAt")),
            asTextOrDefault(field(node, "status"), "never"),
            asNumber(field(node, "imported"), BigDecimal.ZERO).intValue(),
            asNumber(field(node, "duplicates"), BigDecimal.ZERO).intValue(),
            asNumber(field(node, "filesStored"), BigDecimal.ZERO).intValue(),
            asNumber(field(node, "review"), BigDecimal.ZERO).intValue(),
            asStringList(field(node, "errors"), List.of())
        );
    }

    private JsonNode field(JsonNode node, String key) {
        return node != null ? node.get(key) : null;
    }

    private String asTextOrDefault(JsonNode node, String fallback) {
        return node != null && node.isString() ? node.asString("") : fallback;
    }
}
