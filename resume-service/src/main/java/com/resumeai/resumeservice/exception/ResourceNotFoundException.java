package com.resumeai.resumeservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a resume (or any resource) is not found — HTTP 404 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
    public ResourceNotFoundException(String name, String field, Object value) {
        super(String.format("%s not found with %s: '%s'", name, field, value));
    }
}
