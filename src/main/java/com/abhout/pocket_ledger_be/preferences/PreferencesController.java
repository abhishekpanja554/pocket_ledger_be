package com.abhout.pocket_ledger_be.preferences;

import com.abhout.pocket_ledger_be.user.UserPrincipal;
import com.abhout.pocket_ledger_be.web.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {
    private final PreferencesService preferencesService;
    PreferencesController(PreferencesService preferencesService){
        this.preferencesService = preferencesService;
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PreferencesResponse>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody JsonNode body
    ){
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }
        if(body == null || !body.isObject()){
            return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_BODY", "Expected a JSON object."));
        }
        PreferencesResponse response = preferencesService.updatePreferences(userPrincipal.getUser(), body);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
