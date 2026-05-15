package com.resumeai.resumeservice.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ResumeQuotaExceededException — thrown when a FREE user tries to create
 * a 4th resume (limit is 3). HTTP 402 Payment Required.
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class ResumeQuotaExceededException extends RuntimeException {
    public ResumeQuotaExceededException() {
        super("Free plan allows a maximum of 3 resumes. Please upgrade to Premium for unlimited resumes.");
    }
}
