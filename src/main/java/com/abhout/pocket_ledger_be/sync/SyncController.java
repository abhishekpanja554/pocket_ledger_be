package com.abhout.pocket_ledger_be.sync;

import com.abhout.pocket_ledger_be.setting.DriveSyncMeta;
import com.abhout.pocket_ledger_be.user.UserPrincipal;
import com.abhout.pocket_ledger_be.web.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/sync")
@AllArgsConstructor
public class SyncController {
    private final DriveSyncRunner runner;

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<DriveSyncMeta>> runSync(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ){
        if(userPrincipal == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }

        Optional<DriveSyncMeta> res = runner.runSyncForUser(userPrincipal.getUser());
        if (res.isEmpty()){
            return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                            "DRIVE_NOT_CONFIGURED",
                            "Drive sync is not configured. Set a Drive folder in Preferences first."
                    )
            );
        }
        return ResponseEntity.ok(ApiResponse.success(res.get()));
    }
}
