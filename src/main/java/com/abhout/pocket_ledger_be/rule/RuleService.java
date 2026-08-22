package com.abhout.pocket_ledger_be.rule;

import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RuleService {
    private final RuleRepository ruleRepository;

    private List<Rule> normalizeRules(User user,
                                      JsonNode node) {
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

    @Transactional
    public void replaceRules(User user, JsonNode rulesNode) {
        List<Rule> normalized = normalizeRules(user, rulesNode);
        ruleRepository.deleteAll(ruleRepository.findByUserIdOrderByCreatedAtAsc(user.getId()));
        ruleRepository.saveAll(normalized);
    }
}
