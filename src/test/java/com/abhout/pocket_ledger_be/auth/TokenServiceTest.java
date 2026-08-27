package com.abhout.pocket_ledger_be.auth;

import com.abhout.pocket_ledger_be.auth.exceptions.InvalidTokenException;
import com.abhout.pocket_ledger_be.auth.models.TokenPurpose;
import com.abhout.pocket_ledger_be.auth.services.TokenService;
import com.abhout.pocket_ledger_be.user.User;
import com.abhout.pocket_ledger_be.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void consumingATokenTwiceFailsOnTheSecondAttempt() {
        User user = userRepository.save(new User("token-test@example.com", "irrelevant-hash"));

        String rawToken = tokenService.issue(user, TokenPurpose.VERIFY_EMAIL, Duration.ofMinutes(30));

        User consumedBy = tokenService.consume(rawToken, TokenPurpose.VERIFY_EMAIL);
        assertThat(consumedBy.getId()).isEqualTo(user.getId());

        assertThatThrownBy(() -> tokenService.consume(rawToken, TokenPurpose.VERIFY_EMAIL))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void issuingANewTokenInvalidatesThePreviousOneForTheSamePurpose() {
        User user = userRepository.save(new User("token-test-2@example.com", "irrelevant-hash"));

        String firstToken = tokenService.issue(user, TokenPurpose.RESET_PASSWORD, Duration.ofMinutes(30));
        String secondToken = tokenService.issue(user, TokenPurpose.RESET_PASSWORD, Duration.ofMinutes(30));

        assertThatThrownBy(() -> tokenService.consume(firstToken, TokenPurpose.RESET_PASSWORD))
                .isInstanceOf(InvalidTokenException.class);

        User consumedBy = tokenService.consume(secondToken, TokenPurpose.RESET_PASSWORD);
        assertThat(consumedBy.getId()).isEqualTo(user.getId());
    }
}
