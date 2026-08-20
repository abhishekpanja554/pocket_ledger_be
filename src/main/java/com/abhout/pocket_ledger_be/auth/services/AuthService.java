package com.abhout.pocket_ledger_be.auth.services;

import com.abhout.pocket_ledger_be.auth.DTOs.*;
import com.abhout.pocket_ledger_be.auth.components.EmailSender;
import com.abhout.pocket_ledger_be.auth.exceptions.EmailAlreadyExistsException;
import com.abhout.pocket_ledger_be.auth.models.TokenPurpose;
import com.abhout.pocket_ledger_be.user.User;
import com.abhout.pocket_ledger_be.user.UserPrincipal;
import com.abhout.pocket_ledger_be.user.UserRepository;
import com.abhout.pocket_ledger_be.user.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailSender emailSender;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            EmailSender emailSender,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository
    ){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailSender = emailSender;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    public UserResponse register(RegisterRequest req) {
       if(userRepository.existsByEmail(req.email())){
           throw new EmailAlreadyExistsException(req.email());
       }
       String passwordHash = passwordEncoder.encode(req.password());
       User newUser = new User(req.email(), passwordHash);
       userRepository.save(newUser);
       String token = tokenService.issue(newUser, TokenPurpose.VERIFY_EMAIL, Duration.ofHours(24));
       String verificationLink = frontendUrl + "/verify-email?token=" + token;
       emailSender.sendVerificationEmail(req.email(), verificationLink);
       return UserResponse.from(newUser);
    }

    public UserResponse login(LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return UserResponse.from(userPrincipal.getUser());
    }

    public void verifyEmail(VerifyEmailRequest req){
        User user = tokenService.consume(req.token(), TokenPurpose.VERIFY_EMAIL);
        user.verifyEmail();
    }

    public void resendVerification(EmailRequest req){
        userRepository.findByEmail(req.email()).ifPresent(
            user -> {
                if(user.isEmailVerified()){
                    return;
                }
                String token = tokenService.issue(user, TokenPurpose.VERIFY_EMAIL, Duration.ofHours(24));
                String verificationLink = frontendUrl + "/verify-email?token=" + token;
                emailSender.sendVerificationEmail(req.email(), verificationLink);
            }
        );
    }

    public void forgotPassword(EmailRequest req){
        userRepository.findByEmail(req.email()).ifPresent(
                user -> {
                    String token = tokenService.issue(user, TokenPurpose.RESET_PASSWORD, Duration.ofHours(1));
                    String passwordResetLink = frontendUrl + "/reset-password?token=" + token;
                    emailSender.sendPasswordResetEmail(req.email(), passwordResetLink);
                }
        );
    }

    public void resetPassword(ResetPasswordRequest req){
        User user = tokenService.consume(req.token(), TokenPurpose.RESET_PASSWORD);
        user.changePassword(passwordEncoder.encode(req.newPassword()));
    }
}
