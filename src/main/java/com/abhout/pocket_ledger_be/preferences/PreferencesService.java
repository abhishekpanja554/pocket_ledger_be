package com.abhout.pocket_ledger_be.preferences;

import com.abhout.pocket_ledger_be.rule.RuleRepository;
import com.abhout.pocket_ledger_be.rule.RuleResponse;
import com.abhout.pocket_ledger_be.rule.RuleService;
import com.abhout.pocket_ledger_be.setting.SettingService;
import com.abhout.pocket_ledger_be.setting.SettingsResponse;
import com.abhout.pocket_ledger_be.tag.TagRepository;
import com.abhout.pocket_ledger_be.tag.TagResponse;
import com.abhout.pocket_ledger_be.tag.TagService;
import com.abhout.pocket_ledger_be.transaction.TransactionService;
import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PreferencesService {
    private final SettingService settingService;
    private final TagService tagService;
    private final RuleService ruleService;
    private final TransactionService transactionService;
    private final TagRepository tagRepository;
    private final RuleRepository ruleRepository;

    @Transactional
    public PreferencesResponse
    updatePreferences(User user, JsonNode body) {
        if (body.has("tags")) tagService.replaceTags(user, body.get("tags"));
        if (body.has("stripTagsFromTransactions")) {
            transactionService.stripTags(user, body.get("stripTagsFromTransactions"));
        }
        if (body.has("rules")) ruleService.replaceRules(user, body.get("rules"));
        settingService.applyPreferences(user, body);
        SettingsResponse settings = settingService.decode(user);
        List<TagResponse> tags = tagRepository.findByUserIdOrderByNameAsc(user.getId())
            .stream().map(TagResponse::from).toList();
        List<RuleResponse> rules = ruleRepository
            .findByUserIdOrderByCreatedAtAsc(user.getId())
            .stream().map(RuleResponse::from).toList();

        return new PreferencesResponse(settings, tags, rules);
    }
}
