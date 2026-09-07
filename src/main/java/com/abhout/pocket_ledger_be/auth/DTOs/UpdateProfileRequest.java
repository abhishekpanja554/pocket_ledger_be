package com.abhout.pocket_ledger_be.auth.DTOs;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100, message = "fullName must be at most 100 characters")
        String fullName,

        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter uppercase ISO code (e.g. INR, USD)")
        String currency,

        @Pattern(regexp = "^[a-z]{2}-[A-Z]{2}$", message = "locale must match xx-XX format (e.g. en-IN)")
        String locale
) {}
