package com.abhout.pocket_ledger_be.rule;

import java.time.Instant;
import java.util.UUID;

public record RuleResponse(
        UUID id,
        String whenText,
        String thenText,
        boolean enabled,
        Instant createdAt
) {
    public static RuleResponse from(Rule rule) {
        return new RuleResponse(
            rule.getId(),
            rule.getWhenText(),
            rule.getThenText(),
            rule.isEnabled(),
            rule.getCreatedAt()
        );
    }
}
