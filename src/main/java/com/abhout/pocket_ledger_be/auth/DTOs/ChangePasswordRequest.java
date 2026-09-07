package com.abhout.pocket_ledger_be.auth.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(min = 8, max = 70)String oldPassword,
        @NotBlank @Size(min = 8, max = 70) String newPassword
) {}
