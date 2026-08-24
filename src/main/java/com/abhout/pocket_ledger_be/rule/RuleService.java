package com.abhout.pocket_ledger_be.rule;

import com.abhout.pocket_ledger_be.transaction.models.TransactionValidator;
import com.abhout.pocket_ledger_be.transaction.models.ValidTransaction;
import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RuleService {
    private final RuleRepository ruleRepository;
    private static final Pattern WHEN_PREFIX = Pattern.compile(
    "^(when\\s+)?(the\\s+)?(merchant|description|payee|source|name)?\\s*(text\\s+)?(contains|includes|is|equals|matches|starts with|has)?\\s*");
    private static final Pattern QUOTES =
            Pattern.compile("^[\"'`]|[\"'`]$");
    private static final Pattern CATEGORY_PATTERN =
            Pattern.compile("categor(?:y|ise|ize)[^a-z0-9]*(?:as|to|=|:)?\\s*([^,;|]+)",
                   Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_PATTERN = Pattern.compile("tags?[^a-z0-9]*(?:as|to|with|=|:)?\\s*([^,;|]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_PIECE_SPLIT =
            Pattern.compile("[+&]");

    private record ThenResult(String category, List<String> tags) {}

    private List<Rule> normalizeRules(User user, JsonNode node) {
        if (node == null || !node.isArray())
            return List.of();
        List<Rule> result = new ArrayList<>();
        for (int i = 0; i < node.size() && i < 500; i++) {
            JsonNode raw = node.get(i);
            String whenText = str(raw.get("whenText"), "", 200);
            String thenText = str(raw.get("thenText"), "", 200);
            if (whenText.isEmpty() || thenText.isEmpty()) continue;
            result.add(new Rule(user, whenText,thenText, enabledUnlessFalse(raw.get("enabled"))));
        }
        return result;
    }

    private String str(JsonNode node, String fallback, int max) {
        if (node == null || !node.isString()) return fallback;
        String trimmed = node.asString("").trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private boolean enabledUnlessFalse(JsonNode
                                               node) {
        if (node == null || !node.isBoolean())
            return true;
        return node.asBoolean();
    }

    static ThenResult parseThen(String thenText) {
        String text = thenText.trim();
        List<String> rawTags = new ArrayList<>();
        String category = null;

        Matcher categoryMatcher = CATEGORY_PATTERN.matcher(text);
        if (categoryMatcher.find()) category =
                categoryMatcher.group(1).trim();

        Matcher tagMatcher = TAG_PATTERN.matcher(text);
        while (tagMatcher.find()) {
            for (String piece :
                    TAG_PIECE_SPLIT.split(tagMatcher.group(1))) {
                String clean = piece.trim();
                if (!clean.isEmpty()) rawTags.add(clean);
            }
        }

        if (category == null && rawTags.isEmpty() &&
                !text.isEmpty()) category = text;
        return new ThenResult(category,
                normalizeTagList(rawTags));
    }

    private static List<String> normalizeTagList(List<String>
                                                         tags) {
        List<String> result = new ArrayList<>();
        Set<String> seenLower = new LinkedHashSet<>();
        for (String tag : tags) {
            String lower = tag.toLowerCase();
            if (!seenLower.add(lower)) continue;
            result.add(tag);
            if (result.size() >= 25) break;
        }
        return result;
    }

    static String parseWhen(String whenText) {
        String text = whenText.toLowerCase().trim();
        text = WHEN_PREFIX.matcher(text).replaceFirst("");
        text = QUOTES.matcher(text).replaceAll("");
        return text.trim();
    }

    static boolean ruleMatches(Rule rule, String merchant) {
        String needle = parseWhen(rule.getWhenText());
        if (needle.isEmpty()) return false;
        return merchant.toLowerCase().contains(needle);
    }

    public List<Rule> findEnabledRules(UUID userId) {
        return ruleRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .filter(Rule::isEnabled)
                .toList();
    }

    public ValidTransaction applyRules(ValidTransaction tx, List<Rule> enabledRules) {
        String category = tx.category();
        List<String> tags = new ArrayList<>(tx.tags());

        for (Rule rule : enabledRules) {
            if (!ruleMatches(rule, tx.merchant())) continue;
            ThenResult then = parseThen(rule.getThenText());
            if (then.category() != null &&
                    TransactionValidator.DEFAULT_CATEGORY.equals(category)) {
                category = then.category();
            }
            for (String tag : then.tags()) {
                boolean alreadyPresent = tags.stream().anyMatch(
                t -> t.equalsIgnoreCase(tag));
                if (!alreadyPresent) tags.add(tag);
            }
        }

        return new ValidTransaction(tx.date(), tx.merchant(),
                category, tx.amount(),
                tx.type(), tx.account(), normalizeTagList(tags),
                tx.receipt(), tx.source());
    }

    @Transactional
    public void replaceRules(User user, JsonNode rulesNode) {
        List<Rule> normalized = normalizeRules(user, rulesNode);
        ruleRepository.deleteAll(ruleRepository.findByUserIdOrderByCreatedAtAsc(user.getId()));
        ruleRepository.saveAll(normalized);
    }
}
