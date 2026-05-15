package com.resumeai.resumeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ResumeServiceApplication — Entry point for the Resume microservice.
 *
 * This service manages the top-level Resume entity lifecycle:
 *  - Create, Read, Update, Delete resumes
 *  - Duplicate a resume (for creating job-specific variants)
 *  - Update the ATS score (called asynchronously by ai-service)
 *  - Publish / Unpublish to the public gallery
 *  - Track view counts for public resumes
 *  - Enforce FREE tier limit (max 3 resumes)
 *
 * Port: 8082
 * Base URL: http://localhost:8082/api/v1/resumes
 *
 * Dependencies:
 *  - auth-service (8081) — JWT validation
 *  - section-service (8083) — sections are deleted when a resume is deleted
 */
@SpringBootApplication
public class ResumeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResumeServiceApplication.class, args);
    }
}
