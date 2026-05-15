package com.resumeai.resumeservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Resume — JPA Entity representing a single resume document on the platform.
 *
 * Design notes:
 *  - Each Resume belongs to exactly one user (userId — NOT a JPA foreign key,
 *    because users live in a different microservice / database).
 *  - templateId similarly references the template-service's data.
 *  - atsScore is updated asynchronously by the ai-service after each ATS check.
 *  - isPublic controls whether the resume appears in the public gallery.
 *  - status drives the UI (DRAFT vs COMPLETE badge on the dashboard).
 *
 * Maps to the `resumes` table in the `resumeai_resume` database.
 */
@Entity
@Table(
    name = "resumes",
    indexes = {
        // Common query patterns — indexed for performance
        @Index(name = "idx_resumes_user_id",    columnList = "user_id"),
        @Index(name = "idx_resumes_is_public",   columnList = "is_public"),
        @Index(name = "idx_resumes_template_id", columnList = "template_id"),
        @Index(name = "idx_resumes_status",      columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Resume {

    // ── Primary Key ─────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resume_id")
    private Integer resumeId;

    // ── Ownership (cross-service reference) ─────────────────────

    /**
     * The user who owns this resume.
     * Stored as a plain integer — NOT a JPA @ManyToOne because the User
     * entity lives in auth-service's separate database.
     * Cross-service joins are done via HTTP calls, not SQL.
     */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    // ── Resume Content Metadata ──────────────────────────────────

    /** Human-readable title shown on the dashboard (e.g., "Software Engineer Resume") */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * The job role this resume was built for.
     * Used by the AI to generate tailored content and by the ATS checker.
     * Examples: "Senior Java Developer", "Product Manager"
     */
    @Column(name = "target_job_title", length = 200)
    private String targetJobTitle;

    /**
     * ID of the selected template from template-service.
     * Cross-service reference — not a JPA foreign key.
     */
    @Column(name = "template_id")
    private Integer templateId;

    // ── ATS Score ────────────────────────────────────────────────

    /**
     * ATS compatibility score (0–100).
     * 0 = not yet scored
     * Updated asynchronously when the user runs an ATS check via ai-service.
     * The ai-service calls PUT /api/v1/resumes/{id}/ats-score to update this.
     */
    @Column(name = "ats_score")
    @Builder.Default
    private Integer atsScore = 0;

    // ── Status & Lifecycle ───────────────────────────────────────

    /**
     * Resume lifecycle status.
     * DRAFT — still being edited, incomplete
     * COMPLETE — polished and ready to export/share
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ResumeStatus status = ResumeStatus.DRAFT;

    /**
     * Language the resume is written in (e.g., "en", "hi", "fr").
     * Populated when the user uses the AI translation feature.
     */
    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "en";

    // ── Public Gallery ───────────────────────────────────────────

    /**
     * Whether this resume is visible in the public gallery.
     * Users can toggle this at any time.
     * false by default — privacy first.
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Number of times this resume has been viewed in the public gallery.
     * Incremented by incrementViewCount() — NOT updated on every save.
     * Used for analytics and sorting popular resumes.
     */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    // ── Audit Timestamps ─────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enum Definitions ─────────────────────────────────────────

    /**
     * ResumeStatus — the editing state of the resume.
     */
    public enum ResumeStatus {
        DRAFT,    // Still being built — may be missing sections
        COMPLETE  // Ready to export and submit to jobs
    }
}

