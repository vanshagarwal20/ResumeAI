package com.resumeai.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User — JPA Entity representing a registered user in the ResumeAI platform.
 *
 * Maps to the `users` table in the `resumeai_auth` database.
 *
 * Key design decisions:
 *  - `subscriptionPlan` controls access to AI features, templates, exports
 *  - `provider` tracks whether the user logged in via email, Google, or LinkedIn
 *  - `isActive` allows soft-delete (suspend without removing data)
 *  - `role` separates regular users from platform administrators
 */
@Entity
@Table(
    name = "users",
    // Unique constraint on email — one account per email address
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email", name = "uk_users_email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash") // Never print password hash in logs
public class User {

    // ── Primary Key ─────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    // ── Core Identity Fields ─────────────────────────────────────

    /** User's display name — shown in the UI */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Email address — used as the unique login identifier */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * BCrypt-hashed password.
     * NULL for OAuth2 users (they authenticate via Google/LinkedIn).
     */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** Optional phone number for profile completeness */
    @Column(name = "phone", length = 20)
    private String phone;

    // ── Role & Authorization ─────────────────────────────────────

    /**
     * User role — controls what sections of the platform are accessible.
     * Values: USER (default), ADMIN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    // ── OAuth2 Provider ──────────────────────────────────────────

    /**
     * Authentication provider — determines how this user logs in.
     * Values: LOCAL (email/password), GOOGLE, LINKEDIN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    /**
     * Provider's unique ID for the user (e.g., Google sub, LinkedIn member ID).
     * Used during OAuth2 login to look up existing accounts.
     */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    // ── Account Status ───────────────────────────────────────────

    /**
     * Soft-delete flag.
     * false = account suspended/deactivated; user cannot log in.
     * Admins can reactivate by setting this back to true.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ── Subscription ─────────────────────────────────────────────

    /**
     * Subscription tier — the most important business field.
     * FREE: limited AI calls (5/month), 3 resumes, PDF export only
     * PREMIUM: unlimited AI, all templates, DOCX/JSON export, job matching
     *
     * All other services check this field via JWT claims to gate features.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false, length = 20)
    @Builder.Default
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    // ── Audit Timestamps ─────────────────────────────────────────

    /** Automatically set when the record is first inserted */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated whenever the record is modified */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enum Definitions ─────────────────────────────────────────

    /**
     * Role — access control levels within the platform.
     */
    public enum Role {
        USER,   // Regular registered user
        ADMIN   // Platform administrator (full management access)
    }

    /**
     * AuthProvider — which identity provider authenticated this user.
     */
    public enum AuthProvider {
        LOCAL,      // Email + password (stored as bcrypt hash)
        GOOGLE,     // Google OAuth2 Sign-In
        LINKEDIN    // LinkedIn OAuth2 Sign-In
    }

    /**
     * SubscriptionPlan — the billing tier that gates feature access.
     */
    public enum SubscriptionPlan {
        FREE,       // Limited AI calls, 3 resumes, PDF only
        PREMIUM     // Unlimited AI, all templates, all exports, job matching
    }
}

