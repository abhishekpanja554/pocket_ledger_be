package com.abhout.pocket_ledger_be.auth.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(@Email @NotBlank String email) {
}
