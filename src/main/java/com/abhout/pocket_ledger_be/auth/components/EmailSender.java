package com.abhout.pocket_ledger_be.auth.components;

public interface EmailSender {
    void sendVerificationEmail(String to, String verifyLink);
    void sendPasswordResetEmail(String to, String resetLink);
}
