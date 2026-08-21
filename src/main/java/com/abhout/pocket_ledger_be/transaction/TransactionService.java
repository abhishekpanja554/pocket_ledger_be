package com.abhout.pocket_ledger_be.transaction;

import com.abhout.pocket_ledger_be.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private static final int MAX_ERROR_MESSAGES = 20;
    private final TransactionRepository transactionRepository;
    private final TransactionValidator transactionValidator;
    private final JdbcTemplate jDBCTemplate;
    private record Candidate(ValidTransaction value,
                             String fingerprint) {}
    private final ObjectMapper objectMapper;
    private String writeTagsJson(List<String> tags) {
        return objectMapper.writeValueAsString(tags);
    }

    public TransactionWriteResponse insertTransactions(
        User user,
        List<JsonNode> rawInputs,
        boolean applyRules
    ){
        int skipped = 0;
        int duplicates = 0;
        List<String> errorMessages = new ArrayList<>();
        List<ValidTransaction> validated = new ArrayList<>();
        for (JsonNode rawInput : rawInputs) {
            try {
                ValidTransaction transaction = transactionValidator.validate(rawInput);
                validated.add(transaction);
            } catch (TransactionValidationException e) {
                skipped++;
                if (errorMessages.size() < MAX_ERROR_MESSAGES) {
                    errorMessages.add(e.getMessage());
                }
            }
        }

        Set<String> seenFingerprints = new HashSet<>();
        List<Candidate> candidates = new ArrayList<>();
        for (ValidTransaction transaction : validated) {
            String fingerP = transaction.fingerprint();
            if(!seenFingerprints.add(fingerP)){
                duplicates++;
                continue;
            }
            candidates.add(new Candidate(transaction, fingerP));
        }

        Set<String> existingFingerprints = transactionRepository.findByUserIdAndFingerprintIn(
                user.getId(),seenFingerprints).stream().map(Transaction::getFingerprint)
                .collect(Collectors.toSet());
        List<Candidate> toInsert = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if(!existingFingerprints.contains(candidate.fingerprint())){
                toInsert.add(candidate);
            } else {
                duplicates++;
            }
        }

        if(toInsert.isEmpty()){
            return  new TransactionWriteResponse(0,duplicates,skipped,0,errorMessages,List.of());
        }

        Instant now = Instant.now();
        List<UUID> ids = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                INSERT INTO transactions(
                id,
                user_id,
                date,
                merchant,
                category,
                amount,
                type,
                account,
                tags,
                receipt,
                source,
                fingerprint,
                created_at)
                VALUES
                """);
        for (int i = 0; i < toInsert.size(); i++) {
            Candidate candidate = toInsert.get(i);
            ValidTransaction transaction = candidate.value();
            UUID id = UUID.randomUUID();
            ids.add(id);
            if(i > 0) sql.append(", ");
            sql.append("(?,?,?,?,?,?,?,?,?::jsonb,?,?,?,?)");
            args.add(id);
            args.add(user.getId());
            args.add(transaction.date());
            args.add(transaction.merchant());
            args.add(transaction.category());
            args.add(transaction.amount());
            args.add(transaction.type().getWireValue());
            args.add(transaction.account());
            args.add(writeTagsJson(transaction.tags()));
            args.add(transaction.receipt());
            args.add(transaction.source().getWireValue());
            args.add(candidate.fingerprint());
            args.add(Timestamp.from(now));
        }
        sql.append(" ON CONFLICT (user_id, fingerprint) DO NOTHING RETURNING fingerprint;");

        Set<String> totalInserted =  new HashSet<>(
                jDBCTemplate.queryForList(sql.toString(),
                        String.class,
                        args.toArray()
                )
        );

        int inserted = 0;
        int needsReview = 0;
        List<TransactionResponse> rows = new ArrayList<>();
        for ( int i = 0; i < toInsert.size(); i++) {
            Candidate c = toInsert.get(i);
            ValidTransaction transaction = c.value();
            if(totalInserted.contains(c.fingerprint)) {
                inserted++;
                if (TransactionValidator.DEFAULT_CATEGORY.
                        equals(transaction.category()))
                    needsReview++;
                rows.add(new TransactionResponse(
                        ids.get(i),
                        transaction.date(),
                        transaction.merchant(),
                        transaction.category(),
                        transaction.amount(),
                        transaction.type(),
                        transaction.account(),
                        transaction.tags(),
                        transaction.receipt(),
                        transaction.source(),
                        c.fingerprint,
                        now
                ));
            } else {
                duplicates++;
            }
        }
        return new TransactionWriteResponse(inserted,duplicates,skipped,needsReview,errorMessages,rows);
    }
}
