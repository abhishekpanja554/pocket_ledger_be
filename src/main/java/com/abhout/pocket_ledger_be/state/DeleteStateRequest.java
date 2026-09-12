package com.abhout.pocket_ledger_be.state;

import jakarta.validation.constraints.NotBlank;

public record DeleteStateRequest(
        @NotBlank String confirm
) {
}
