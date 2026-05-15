package com.resumeai.auth.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * RegisterRequest — payload sent by the client when creating a new account.
 *
 * Validation annotations ensure bad data is rejected at the controller layer
 * before it ever reaches the database.
 */
@Data                  // Lombok: generates getters, setters, equals, hashCode, toString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    /**
     * User's display name — shown in the dashboard header.
     * Must be between 2 and 100 characters.
     */
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    /**
     * Email address — the unique account identifier.
     * Spring's @Email validates the format (user@domain.tld).
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    /**
     * Plain-text password from the registration form.
     * Will be BCrypt-hashed before storage — NEVER stored as plain text.
     * Minimum 8 characters, max 100 (to avoid bcrypt performance attacks with huge inputs).
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    /** Optional phone number */
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

}

