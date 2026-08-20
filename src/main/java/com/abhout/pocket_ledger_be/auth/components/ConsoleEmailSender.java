package com.abhout.pocket_ledger_be.auth.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleEmailSender implements EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleEmailSender.class);
    @Override
    public void sendVerificationEmail(String to, String verifyLink) {
        logger.info("Verification email to {}: {}", to, verifyLink);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        logger.info("Password reset email to {}: {}", to, resetLink);
    }
}
