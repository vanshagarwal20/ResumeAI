package com.resumeai.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ApiResponse<T> — Generic wrapper for all REST API responses.
 *
 * Every endpoint returns this wrapper so the frontend always gets a consistent
 * JSON structure with a success flag, message, and optional data payload.
 *
 * Success example:
 * {
 *   "success": true,
 *   "message": "Login successful",
 *   "data": { ... },
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * Error example:
 * {
 *   "success": false,
 *   "message": "Invalid credentials",
 *   "data": null,
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * @param <T> The type of the data payload (e.g., AuthResponse, UserProfileResponse)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Don't include null fields in JSON output — keeps responses clean
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** Whether the operation succeeded */
    private boolean success;

    /** Human-readable message for the UI to display */
    private String message;

    /** The actual response payload — null on errors */
    private T data;

    /** When this response was generated — useful for debugging */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ── Static Factory Methods ───────────────────────────────────

    /**
     * Create a success response with data payload.
     *
     * @param message Descriptive success message
     * @param data    The response payload
     * @param <T>     Type of the payload
     * @return ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Create a success response without data (e.g., for DELETE operations).
     *
     * @param message Descriptive success message
     * @param <T>     Type parameter (will be Void)
     * @return ApiResponse with success=true and no data
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Create an error response.
     *
     * @param message Descriptive error message
     * @param <T>     Type parameter
     * @return ApiResponse with success=false
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}

