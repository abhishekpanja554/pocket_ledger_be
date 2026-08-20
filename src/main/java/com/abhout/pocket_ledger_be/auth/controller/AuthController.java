package com.abhout.pocket_ledger_be.auth.controller;

import com.abhout.pocket_ledger_be.auth.DTOs.*;
import com.abhout.pocket_ledger_be.auth.services.AuthService;
import com.abhout.pocket_ledger_be.user.UserPrincipal;
import com.abhout.pocket_ledger_be.user.UserResponse;
import com.abhout.pocket_ledger_be.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @RequestBody @Valid RegisterRequest registerRequest
    ) {
        UserResponse response = authService.register(registerRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @RequestBody @Valid LoginRequest loginRequest,
            HttpServletRequest req, HttpServletResponse res
    ) {
        UserResponse response = authService.login(loginRequest,req,res);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        if(userPrincipal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "You are unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(userPrincipal.getUser())));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody @Valid VerifyEmailRequest request){
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestBody @Valid EmailRequest request){
        authService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody @Valid EmailRequest request){
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest request){
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

}
