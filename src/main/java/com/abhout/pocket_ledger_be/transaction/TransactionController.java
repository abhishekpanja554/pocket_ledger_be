package com.abhout.pocket_ledger_be.transaction;

import com.abhout.pocket_ledger_be.transaction.DTOs.TransactionResponse;
import com.abhout.pocket_ledger_be.transaction.DTOs.TransactionWriteResponse;
import com.abhout.pocket_ledger_be.user.UserPrincipal;
import com.abhout.pocket_ledger_be.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    private List<JsonNode> extractItems(JsonNode body) {
        if(body == null || body.isNull()) return  List.of();

        if (body.isArray()) {
            List<JsonNode> items = new ArrayList<>();
            body.forEach(item -> items.add(item));
            return items;
        }

        if(body.isObject()
                && body.has("transactions")
                && body.get("transactions").isArray()) {
            List<JsonNode> items = new ArrayList<>();
            body.get("transactions").forEach(item -> items.add(item));
            return items;
        }

        return List.of(body);
    }

    private boolean resolveApplyRules(JsonNode body){
        if (body == null || !body.isObject()) {
            return false;
        }

        JsonNode applyRulesNode = body.get("applyRules");
        if (applyRulesNode != null && applyRulesNode.isBoolean() && applyRulesNode.asBoolean()) {
            return true;
        }

        JsonNode sourceNode = body.get("source");
        return sourceNode != null && sourceNode.isString() && !"manual".equals(sourceNode.asString());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionWriteResponse>> create (
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody JsonNode body
    ){
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }

        List<JsonNode> items = extractItems(body);
        if(items.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("EMPTY_BATCH", "No transactions provided"));
        }

        if(items.size() > 2000){
            return ResponseEntity.badRequest().body(ApiResponse.error("TOO_MANY_TRANSACTIONS",
                    "Import batches are limited to 2,000 rows at a time."));
        }

        boolean applyRules = resolveApplyRules(body);

        TransactionWriteResponse response = transactionService.insertTransactions(userPrincipal.getUser(), items, applyRules);
        boolean allFailedValidation = response.inserted()
                == 0
                && response.rows().isEmpty()
                && response.skipped() == items.size();

        return ResponseEntity.status(allFailedValidation ?
                    HttpStatus.BAD_REQUEST : HttpStatus.OK)
                    .body(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    public  ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id,
            @RequestBody JsonNode body){
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }
        TransactionResponse response = transactionService.updateTransaction(userPrincipal.getUser(), id, body);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id){
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }
        transactionService.deleteTransaction(userPrincipal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success());
    }

}
