package com.abhout.pocket_ledger_be.auth.DTOs;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank String token) {
}
