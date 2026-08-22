package com.abhout.pocket_ledger_be.preferences;

import com.abhout.pocket_ledger_be.rule.RuleResponse;
import com.abhout.pocket_ledger_be.setting.SettingsResponse;
import com.abhout.pocket_ledger_be.tag.TagResponse;

import java.util.List;

public record PreferencesResponse(
        SettingsResponse settings,
        List<TagResponse> tags,
        List<RuleResponse> rules
) {
}
