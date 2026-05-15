package com.resumeai.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * InvalidCredentialsException — thrown when login credentials are wrong.
 * Maps to HTTP 401 Unauthorized.
 *
 * NOTE: We intentionally use a vague message ("Invalid email or password")
 * rather than "Email not found" or "Wrong password" — this prevents
 * attackers from enumerating valid email addresses.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}

