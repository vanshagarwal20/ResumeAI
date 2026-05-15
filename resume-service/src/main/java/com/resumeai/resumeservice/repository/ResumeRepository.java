package com.resumeai.resumeservice.repository;

import com.resumeai.resumeservice.entity.Resume;
import com.resumeai.resumeservice.entity.Resume.ResumeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ResumeRepository — Data Access Layer for the Resume entity.
 *
 * Spring Data JPA generates all SQL at startup from method names.
 * Custom @Query methods are used for targeted updates (avoid loading
 * the full entity just to change one field).
 */
@Repository
public interface ResumeRepository extends JpaRepository<Resume, Integer> {

    // ── User-scoped queries ──────────────────────────────────────

    /**
     * Get all resumes belonging to a specific user, newest first.
     * Used to render the user's dashboard resume list.
     *
     * @param userId The owner's user ID
     * @return Ordered list of user's resumes
     */
    List<Resume> findByUserIdOrderByCreatedAtDesc(Integer userId);

    Optional<Resume> findFirstByUserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * Find a specific resume by ID, verifying it belongs to the given user.
     * Used for authorization checks — prevents users from accessing each other's resumes.
     *
     * @param resumeId The resume's primary key
     * @param userId   The requesting user's ID
     * @return Optional — empty if resume doesn't exist OR belongs to a different user
     */
    Optional<Resume> findByResumeIdAndUserId(Integer resumeId, Integer userId);

    /**
     * Count how many resumes a user has created.
     * Used to enforce the FREE tier limit of 3 resumes.
     *
     * @param userId The user's ID
     * @return Total number of resumes owned by this user
     */
    long countByUserId(Integer userId);

    // ── Status-based queries ─────────────────────────────────────

    /**
     * Get all resumes with a specific status for a user.
     * Used to filter dashboard view (e.g., show only DRAFT resumes).
     *
     * @param userId The owner's user ID
     * @param status DRAFT or COMPLETE
     * @return Filtered list of resumes
     */
    List<Resume> findByUserIdAndStatus(Integer userId, ResumeStatus status);

    /**
     * Get all resumes targeting a specific job title (within a user's account).
     * Useful for searching "all my Java Developer resumes".
     *
     * @param userId       The owner's user ID
     * @param targetJobTitle The job title to search for
     * @return Matching resumes
     */
    List<Resume> findByUserIdAndTargetJobTitleContainingIgnoreCase(
            Integer userId, String targetJobTitle);

    // ── Public gallery queries ───────────────────────────────────

    /**
     * Get all publicly shared resumes, ordered by view count (most popular first).
     * Powers the public resume gallery page.
     *
     * @param isPublic pass true to get public resumes
     * @return List of public resumes, sorted by popularity
     */
    List<Resume> findByIsPublicOrderByViewCountDesc(Boolean isPublic);

    /**
     * Get resumes by template ID — used by template-service to show
     * "X resumes use this template" on the template detail page.
     *
     * @param templateId The template's ID
     * @return Resumes using that template
     */
    List<Resume> findByTemplateId(Integer templateId);

    // ── Targeted update queries ──────────────────────────────────

    /**
     * Update only the ATS score field without loading the full entity.
     * Called by ai-service after completing an ATS compatibility check.
     *
     * @param resumeId The resume to update
     * @param atsScore The new score (0-100)
     */
    @Modifying
    @Query("UPDATE Resume r SET r.atsScore = :atsScore WHERE r.resumeId = :resumeId")
    void updateAtsScore(@Param("resumeId") Integer resumeId,
                        @Param("atsScore") Integer atsScore);

    /**
     * Toggle the isPublic flag to publish a resume to the gallery.
     *
     * @param resumeId The resume to publish
     * @param isPublic true to publish, false to unpublish
     */
    @Modifying
    @Query("UPDATE Resume r SET r.isPublic = :isPublic WHERE r.resumeId = :resumeId")
    void updateIsPublic(@Param("resumeId") Integer resumeId,
                        @Param("isPublic") Boolean isPublic);

    /**
     * Atomically increment the view count by 1.
     * Using a direct UPDATE avoids race conditions that would occur if we
     * did: load → increment in Java → save.
     *
     * @param resumeId The resume being viewed
     */
    @Modifying
    @Query("UPDATE Resume r SET r.viewCount = r.viewCount + 1 WHERE r.resumeId = :resumeId")
    void incrementViewCount(@Param("resumeId") Integer resumeId);

    /**
     * Update the resume status.
     *
     * @param resumeId The resume to update
     * @param status   DRAFT or COMPLETE
     */
    @Modifying
    @Query("UPDATE Resume r SET r.status = :status WHERE r.resumeId = :resumeId")
    void updateStatus(@Param("resumeId") Integer resumeId,
                      @Param("status") ResumeStatus status);

    // ── Analytics queries ────────────────────────────────────────

    /**
     * Count total resumes by status — used in Admin analytics.
     *
     * @param status The status to count
     * @return Count of resumes with that status
     */
    long countByStatus(ResumeStatus status);

    /**
     * Count all public resumes — admin analytics.
     *
     * @param isPublic pass true to count public resumes
     * @return Count
     */
    long countByIsPublic(Boolean isPublic);
}

