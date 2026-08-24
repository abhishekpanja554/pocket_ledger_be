package com.abhout.pocket_ledger_be.state;

import com.abhout.pocket_ledger_be.document.DTOs.DocumentResponse;
import com.abhout.pocket_ledger_be.rule.RuleResponse;
import com.abhout.pocket_ledger_be.setting.SettingsResponse;
import com.abhout.pocket_ledger_be.tag.TagResponse;
import com.abhout.pocket_ledger_be.transaction.DTOs.TransactionResponse;

import java.util.List;

public record StateResponse(
    List<TransactionResponse> transactions,
    List<TagResponse> tags,
    List<RuleResponse> rules,
    SettingsResponse settings,
    List<DocumentResponse> documents
) {
}

