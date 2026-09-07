package com.abhout.pocket_ledger_be.auth.services;

import com.abhout.pocket_ledger_be.auth.DTOs.*;
import com.abhout.pocket_ledger_be.auth.components.EmailSender;
import com.abhout.pocket_ledger_be.auth.exceptions.AccountDeletionFailedException;
import com.abhout.pocket_ledger_be.auth.exceptions.EmailAlreadyExistsException;
import com.abhout.pocket_ledger_be.auth.exceptions.InvalidPasswordException;
import com.abhout.pocket_ledger_be.auth.models.TokenPurpose;
import com.abhout.pocket_ledger_be.document.DocumentRepository;
import com.abhout.pocket_ledger_be.document.models.Document;
import com.abhout.pocket_ledger_be.storage.DocumentStorageProvider;
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
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailSender emailSender;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageProvider documentStorageProvider;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            EmailSender emailSender,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            FindByIndexNameSessionRepository<? extends Session> sessionRepository,
            DocumentRepository documentRepository,
            DocumentStorageProvider documentStorageProvider
    ){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.emailSender = emailSender;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionRepository = sessionRepository;
        this.documentRepository = documentRepository;
        this.documentStorageProvider = documentStorageProvider;
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

    public void changePassword(ChangePasswordRequest req, User user){
        if( !passwordEncoder.matches(req.oldPassword(), user.getPasswordHash()) ){
            throw new InvalidPasswordException("Old password is incorrect");
        }
        Map<String, ? extends Session> sessions =
                sessionRepository.findByIndexNameAndIndexValue(
                        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                        user.getEmail()
                );
        sessions.keySet().forEach(sessionRepository::deleteById);
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    public void deleteAccount(DeleteAccountRequest req, User user){
        if( !passwordEncoder.matches(req.password(), user.getPasswordHash()) ){
            throw new InvalidPasswordException("Password is incorrect");
        }
        List<Document> docs = documentRepository.findByUserId(user.getId());
        List<String> failed = new ArrayList<>();
        for (Document doc : docs) {
            try {
                documentStorageProvider.delete(doc.getObjectKey());
            } catch (RuntimeException e) {
                failed.add(doc.getObjectKey());
            }
        }
        if (!failed.isEmpty()) {
            throw new AccountDeletionFailedException(
                    failed.size() + " of " + docs.size() + " files could not be deleted. Please try again."
            );
        }
        userRepository.delete(user);
        Map<String, ? extends Session> sessions =
                sessionRepository.findByIndexNameAndIndexValue(
                        FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                        user.getEmail()
                );
        sessions.keySet().forEach(sessionRepository::deleteById);
    }

    public UserResponse updateProfile(
            UpdateProfileRequest req,
            User user,
            HttpServletRequest request,
            HttpServletResponse response
    ){
        user.updateProfile(
                req.fullName(),
                req.locale(),
                req.currency()
        );
        userRepository.save(user);
        UserPrincipal newPrincipal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                newPrincipal, null, newPrincipal.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
        return UserResponse.from(user);
    }
}
