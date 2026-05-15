package com.resumeai.export.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ExportJob stores export history so the user can later
 * see what resume was exported and in which format.
 */
@Entity
@Table(name = "export_jobs",
        indexes = {
                @Index(name = "idx_export_user", columnList = "user_id"),
                @Index(name = "idx_export_resume", columnList = "resume_id"),
                @Index(name = "idx_export_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer exportId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "resume_id", nullable = false)
    private Integer resumeId;

    @Column(name = "template_id", nullable = false)
    private Integer templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false, length = 20)
    private ExportFormat exportFormat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExportStatus status;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "download_url", length = 1000)
    private String downloadUrl;

    @Lob
    @Column(name = "generated_html", columnDefinition = "LONGTEXT")
    private String generatedHtml;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ExportFormat {
        PDF,
        HTML,
        DOCX,
        JSON
    }

    public enum ExportStatus {
        COMPLETED,
        FAILED
    }
}
