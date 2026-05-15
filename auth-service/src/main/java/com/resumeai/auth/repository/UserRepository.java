package com.resumeai.auth.repository;

import com.resumeai.auth.entity.User;
import com.resumeai.auth.entity.User.AuthProvider;
import com.resumeai.auth.entity.User.Role;
import com.resumeai.auth.entity.User.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository — Data Access Layer for the User entity.
 *
 * Spring Data JPA automatically implements all methods at runtime.
 * No SQL needed — method names are parsed to generate queries.
 *
 * Naming convention:
 *   findBy<Field>      → SELECT WHERE field = ?
 *   existsBy<Field>    → SELECT COUNT(*) > 0 WHERE field = ?
 *   countBy<Field>     → SELECT COUNT(*) WHERE field = ?
 *   deleteBy<Field>    → DELETE WHERE field = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // ── Lookup Methods ───────────────────────────────────────────

    /**
     * Find a user by their email address.
     * Used during login to load the user for password verification.
     *
     * @param email The email address to search for
     * @return Optional<User> — empty if no user with this email exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their primary key (userId).
     * Note: JpaRepository already provides findById(Integer),
     * but this is more readable in service code.
     *
     * @param userId The user's primary key
     * @return Optional<User>
     */
    Optional<User> findByUserId(Integer userId);

    /**
     * Find a user by their OAuth2 provider and provider-specific ID.
     * Used during OAuth2 login: first check if this Google/LinkedIn account
     * is already linked to a ResumeAI account.
     *
     * @param provider  e.g., GOOGLE or LINKEDIN
     * @param providerId The unique ID from the OAuth2 provider
     * @return Optional<User>
     */
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    // ── Existence Checks ─────────────────────────────────────────

    /**
     * Check if an email is already registered.
     * Used during registration to prevent duplicate accounts.
     *
     * @param email The email to check
     * @return true if the email is already in use
     */
    boolean existsByEmail(String email);

    // ── Admin / Filter Queries ───────────────────────────────────

    /**
     * Get all users with a specific role.
     * Used by Admin to list all ADMINs or all regular USERs.
     *
     * @param role USER or ADMIN
     * @return List of matching users
     */
    List<User> findAllByRole(Role role);

    /**
     * Get all users on a specific subscription plan.
     * Used for analytics ("how many premium users?") and bulk notifications.
     *
     * @param subscriptionPlan FREE or PREMIUM
     * @return List of matching users
     */
    List<User> findBySubscriptionPlan(SubscriptionPlan subscriptionPlan);

    /**
     * Get users by their active/suspended status.
     * Used by Admin to view suspended accounts.
     *
     * @param isActive true = active accounts; false = suspended
     * @return List of matching users
     */
    List<User> findByIsActive(Boolean isActive);

    // ── Custom Update Queries ────────────────────────────────────

    /**
     * Directly update the subscription plan without loading the full entity.
     * More efficient than load → modify → save for a single field update.
     *
     * @param userId           The user to update
     * @param subscriptionPlan The new plan (FREE or PREMIUM)
     */
    @Modifying
    @Query("UPDATE User u SET u.subscriptionPlan = :plan WHERE u.userId = :userId")
    void updateSubscriptionPlan(@Param("userId") Integer userId,
                                 @Param("plan") SubscriptionPlan subscriptionPlan);

    /**
     * Directly toggle the isActive flag (suspend or reactivate a user).
     *
     * @param userId   The user to update
     * @param isActive false to suspend; true to reactivate
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.userId = :userId")
    void updateIsActive(@Param("userId") Integer userId,
                        @Param("isActive") Boolean isActive);

    // ── Analytics ────────────────────────────────────────────────

    /**
     * Count users by subscription plan — used in Admin analytics dashboard.
     *
     * @param subscriptionPlan FREE or PREMIUM
     * @return count of users on that plan
     */
    long countBySubscriptionPlan(SubscriptionPlan subscriptionPlan);

    /**
     * Count all active (non-suspended) users.
     *
     * @param isActive pass true to count active users
     * @return count of active users
     */
    long countByIsActive(Boolean isActive);
}

