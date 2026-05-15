package com.resumeai.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * ChangePasswordRequest — payload for changing the user's password.
 * Requires the current password for verification (prevents CSRF attacks).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    /** The user's current password — verified before allowing the change */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /** The desired new password */
    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    private String newPassword;
}
