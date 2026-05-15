package com.resumeai.section.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_sections",
        indexes = {
                @Index(name = "idx_section_resume_user", columnList = "resume_id,user_id"),
                @Index(name = "idx_section_type", columnList = "section_type"),
                @Index(name = "idx_section_order", columnList = "resume_id,display_order")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sectionId;

    @Column(nullable = false)
    private Integer resumeId;

    @Column(nullable = false)
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private SectionType sectionType;

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private Boolean isVisible;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isVisible == null) {
            this.isVisible = true;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum SectionType {
        SUMMARY,
        EXPERIENCE,
        EDUCATION,
        SKILLS,
        PROJECTS,
        CERTIFICATIONS,
        ACHIEVEMENTS,
        LANGUAGES,
        EXTRA_CURRICULAR
    }
}