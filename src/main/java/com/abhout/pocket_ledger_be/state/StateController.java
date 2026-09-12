package com.abhout.pocket_ledger_be.state;

import com.abhout.pocket_ledger_be.user.UserPrincipal;
import com.abhout.pocket_ledger_be.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/state")
public class StateController {
    private final StateService stateService;

    StateController(StateService stateService){
        this.stateService = stateService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<StateResponse>> getState(@AuthenticationPrincipal UserPrincipal userPrincipal){
        if(userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }

        StateResponse response = stateService.getState(userPrincipal.getUser());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<DeleteStateResponse>> deleteState(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid DeleteStateRequest request
    ){
        if(userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }

        int objectsDeleted = stateService.deleteState(userPrincipal.getUser(), request);
        return ResponseEntity.ok(ApiResponse.success(new DeleteStateResponse(objectsDeleted)));
    }
}
