package com.abhout.pocket_ledger_be.auth;

import com.abhout.pocket_ledger_be.user.User;
import com.abhout.pocket_ledger_be.user.UserRepository;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class TokenService {
    private final AuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    TokenService(AuthTokenRepository authTokenRepository, UserRepository userRepository) {
        this.authTokenRepository = authTokenRepository;
        this.userRepository = userRepository;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String issue(User user, TokenPurpose tokenPurpose, Duration validFor) {
        authTokenRepository.invalidateActive(user, tokenPurpose);
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
        String tokenHash = hash(rawToken);

        AuthToken authToken = new AuthToken(
                user,
                tokenHash,
                tokenPurpose,
                Instant.now().plus(validFor)
        );
        authTokenRepository.save(authToken);
        return rawToken;
    }

    public User consume(String rawToken, TokenPurpose tokenPurpose){
        String tokenHash = hash(rawToken);
        AuthToken authToken = authTokenRepository
            .findByTokenHashAndPurpose(tokenHash,tokenPurpose)
            .filter(token -> token.isValid())
            .orElseThrow(() -> new InvalidTokenException(
                    "Token is invalid, expired, or already used"));
        authToken.markUsed();
        return  authToken.getUser();
    }
}
