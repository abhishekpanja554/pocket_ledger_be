package com.abhout.pocket_ledger_be.web;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ErrorDetails {
    private String code;
    private String message;
    private List<String> details;

    public ErrorDetails(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
