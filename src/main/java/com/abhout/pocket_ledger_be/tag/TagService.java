package com.abhout.pocket_ledger_be.tag;

import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    private List<String> normalizeNames(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        Set<String> seenLower = new LinkedHashSet<>();
        for (JsonNode item : node) {
            if (!item.isString()) continue;
            String trimmed = item.asString("").trim();
            if (trimmed.isEmpty()) continue;
            if (!seenLower.add(trimmed.toLowerCase())) continue;
            result.add(trimmed);
        }
        return result;
    }

    @Transactional
    public void replaceTags(User user, JsonNode tagsNode) {
        List<String> names = normalizeNames(tagsNode);
        Set<String> keepLower = names.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<Tag> existing = tagRepository.findByUserIdOrderByNameAsc(user.getId());
        List<Tag> toDelete = existing.stream()
                .filter(tag -> !keepLower.contains(tag.getName().toLowerCase()))
                .toList();
        tagRepository.deleteAll(toDelete);
        Set<String> keptLower = existing.stream()
                .filter(tag -> keepLower.contains(tag.getName().toLowerCase()))
                .map(tag -> tag.getName().toLowerCase())
                .collect(Collectors.toSet());

        List<Tag> toInsert = names.stream()
                .filter(name -> !keptLower.contains(name.toLowerCase()))
                .map(name -> new Tag(user, name))
                .toList();
        tagRepository.saveAll(toInsert);
    }
}
