package com.abhout.pocket_ledger_be.setting;

import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
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
    private static final Set<String> CADENCES =
            Set.of("weekly", "biweekly", "monthly",
                    "quarterly", "annual");
    private static final Pattern TIME_PATTERN =
            Pattern.compile("^\\d{2}:\\d{2}$");

    private String str(JsonNode node, String fallback, int max) {
        if (node == null || !node.isString()) return fallback;
        String trimmed = node.asString("").trim();
        return trimmed.length() > max ?
            trimmed.substring(0, max) : trimmed;
    }

    private String str(JsonNode node) {
        return str(node, "", 120);
    }

    private String orGenerateId(JsonNode node) {
        String id = str(node);
        return id.isEmpty() ? UUID.randomUUID().toString() : id;
    }

    private String createdAtOrNow(JsonNode node) {
        String createdAt = str(node);
        return createdAt.isEmpty() ?
                Instant.now().toString() : createdAt;
    }

    private String isoDateOrNull(JsonNode node) {
        String s = str(node, "", 10);
        return s.matches("\\d{4}-\\d{2}-\\d{2}") ?
                s : null;
    }

    private String cadence(JsonNode node) {
        if (node != null && node.isString() &&
                CADENCES.contains(node.asString("")))
        {
            return node.asString("");
        }
        return "monthly";
    }

    private boolean activeUnlessFalse(JsonNode node)
    {
        if (node == null || !node.isBoolean()) return true;
        return node.asBoolean();
    }

    private JsonNode normalizeGoals(JsonNode node) {
        ArrayNode result = objectMapper.createArrayNode();
        if (node == null || !node.isArray()) return result;
        for (int i = 0; i < node.size() && i < 500; i++) {
            JsonNode raw = node.get(i);
            String name = str(raw.get("name"));
            if (name.isEmpty()) continue;
            ObjectNode goal = objectMapper.createObjectNode();
            goal.put("id", orGenerateId(raw.get("id")));
            goal.put("name", name);
            goal.put("target", money(raw.get("target")));
            goal.put("current", money(raw.get("current")));
            String dueDate = isoDateOrNull(raw.get("dueDate"));
            if (dueDate != null) goal.put("dueDate", dueDate);
            String note = str(raw.get("note"), "",400);
            if (!note.isEmpty()) goal.put("note", note);
            goal.put("createdAt", createdAtOrNow(raw.get("createdAt")));
            result.add(goal);
        }
        return result;
    }

    private JsonNode normalizeBudgets(JsonNode node)
    {
        ArrayNode result = objectMapper.createArrayNode();
        if (node == null || !node.isArray()) return result;
        for (int i = 0; i < node.size() && i < 500; i++) {
            JsonNode raw = node.get(i);
            String category = str(raw.get("category"));
            if (category.isEmpty()) continue;
            ObjectNode budget = objectMapper.createObjectNode();
            budget.put("id", orGenerateId(raw.get("id")));
            budget.put("category", category);
            budget.put("limit", money(raw.get("limit")));
            budget.put("active", activeUnlessFalse(raw.get("active")));
            budget.put("createdAt", createdAtOrNow(raw.get("createdAt")));
            result.add(budget);
        }
        return result;
    }

    private JsonNode normalizeRecurring(JsonNode node) {
        ArrayNode result = objectMapper.createArrayNode();
        if (node == null || !node.isArray()) return result;
        for (int i = 0; i < node.size() && i < 500; i++) {
            JsonNode raw = node.get(i);
            String name = str(raw.get("name"));
            if (name.isEmpty()) continue;
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("id", orGenerateId(raw.get("id")));
            entry.put("name", name);
            String category = str(raw.get("category"));
            entry.put("category", category.isEmpty() ? "Other" : category);
            entry.put("amount", money(raw.get("amount")));
            entry.put("cadence", cadence(raw.get("cadence")));
            entry.put("nextDate", Objects.requireNonNullElse(isoDateOrNull(raw.get("nextDate")), ""));
            String account = str(raw.get("account"));
            if (!account.isEmpty()) entry.put("account", account);
            entry.put("active", activeUnlessFalse(raw.get("active")));
            entry.put("createdAt", createdAtOrNow(raw.get("createdAt")));
            result.add(entry);
        }
        return result;
    }

    private JsonNode normalizeSubscriptions(JsonNode
                                                    node) {
        ArrayNode result =
                objectMapper.createArrayNode();
        if (node == null || !node.isArray()) return
                result;
        for (int i = 0; i < node.size() && i < 500;
             i++) {
            JsonNode raw = node.get(i);
            String name = str(raw.get("name"));
            if (name.isEmpty()) continue;
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("id", orGenerateId(raw.get("id")));
            entry.put("name", name);
            String group = str(raw.get("group"));
            entry.put("group", group.isEmpty() ? "Subscriptions" : group);
            entry.put("amount", money(raw.get("amount")));
            entry.put("cadence", cadence(raw.get("cadence")));
            entry.put("nextRenewal",
                    Objects.requireNonNullElse(isoDateOrNull(raw.get("nextRenewal")), ""));
            String account = str(raw.get("account"));
            if (!account.isEmpty()) entry.put("account", account);
            entry.put("active", activeUnlessFalse(raw.get("active")));
            entry.put("createdAt", createdAtOrNow(raw.get("createdAt")));
            result.add(entry);
        }
        return result;
    }

    private void writeIfPresent(
        User user,
        JsonNode body,
        String key,
        Function<JsonNode, Object> normalize
    ) {
        if (!body.has(key)) return;
        Object normalized = normalize.apply(body.get(key));
        settingRepository.save(new Setting(user,
                key, objectMapper.writeValueAsString(normalized)));
    }

    private List<String> normalizeNames(JsonNode node) {
        if (node == null || !node.isArray())
            return List.of();
        List<String> result = new ArrayList<>();
        Set<String> seenLower = new LinkedHashSet<>();
        for (JsonNode item : node) {
            if (!item.isString()) continue;
            String trimmed =
                    item.asString("").trim();
            if (trimmed.isEmpty()) continue;
            if
            (!seenLower.add(trimmed.toLowerCase()))
                continue;   // case-insensitive dedupe
            result.add(trimmed);
        }
        return result;
    }

    private BigDecimal money(JsonNode node) {
        return asNumber(node, BigDecimal.ZERO).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private JsonNode normalizeDriveFolder(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        String id = str(node.get("id"));
        if (id.isEmpty()) return null;
        ObjectNode folder = objectMapper.createObjectNode();
        folder.put("id", id);
        String name = str(node.get("name"));
        folder.put("name", name.isEmpty() ?
                "Pocket Ledger Financial Inbox" : name);
        folder.put("url", str(node.get("url"), "", 400));
        return folder;
    }

    private JsonNode normalizeDriveSchedule(JsonNode node) {
        String time = str(field(node, "time"));
        ObjectNode schedule =objectMapper.createObjectNode();
        schedule.put("time",
                TIME_PATTERN.matcher(time).matches() ? time : "08:00");
        schedule.put("timezone", str(field(node,"timezone"),
                defaultTimeZone, 64));
        schedule.put("cadence", "daily");
        return schedule;
    }

    public SettingsResponse decode(User user) {
        Map<String, JsonNode> raw = settingRepository.findByUserId(user.getId()).stream()
            .collect(Collectors.toMap(Setting::getKey, this::parseValue));

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

    @Transactional
    public void applyPreferences(User user, JsonNode body){
        writeIfPresent(user, body,"categories", this::normalizeNames);
        writeIfPresent(user, body, "accounts", this::normalizeNames);
        writeIfPresent(user, body, "dismissedPatterns", this::normalizeNames);
        writeIfPresent(user, body, "assets", this::money);
        writeIfPresent(user, body, "liabilities", this::money);
        writeIfPresent(user, body, "netWorthConfigured", this::asStrictBoolean);
        writeIfPresent(user, body, "freshStart", this::asStrictBoolean);
        writeIfPresent(user, body, "selectedPeriod", this::asPeriod);
        writeIfPresent(user, body, "goals", this::normalizeGoals);
        writeIfPresent(user, body, "budgets", this::normalizeBudgets);
        writeIfPresent(user, body, "subscriptions", this::normalizeSubscriptions);
        writeIfPresent(user, body, "recurring", this::normalizeRecurring);
        writeIfPresent(user, body, "driveFolder", this::normalizeDriveFolder);
        writeIfPresent(user, body, "driveSchedule", this::normalizeDriveSchedule);
    }
}
