package com.abhout.pocket_ledger_be.tag;

import java.time.Instant;

public record TagResponse(
        String name,
        Instant createdAt
) {
    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.getName(), tag.getCreatedAt());
    }
}
