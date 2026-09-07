package com.abhout.pocket_ledger_be.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, name = "password_hash")
    private  String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "email_verified",nullable = false)
    private boolean emailVerified;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "locale", nullable = false)
    private String locale = "en-IN";

    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void verifyEmail(){
        this.emailVerified = true;
    }

    public void changePassword(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public void updateProfile(String fullName, String locale, String currency){
        if (fullName != null) this.fullName = fullName;
        if (locale != null) this.locale = locale;
        if (currency != null) this.currency = currency;
    }
}
