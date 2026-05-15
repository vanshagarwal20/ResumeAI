package com.resumeai.template.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ResumeTemplate stores layout and business metadata
 * about a resume template shown in the UI.
 */
@Entity
@Table(name = "resume_templates",
        indexes = {
                @Index(name = "idx_template_public", columnList = "is_public"),
                @Index(name = "idx_template_premium", columnList = "is_premium"),
                @Index(name = "idx_template_category", columnList = "category")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer templateId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 1000)
    private String previewImageUrl;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String htmlStructure;

    @Column(nullable = false)
    private Boolean isPremium;

    @Column(nullable = false)
    private Boolean isPublic;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.isPublic == null) {
            this.isPublic = true;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isPremium == null) {
            this.isPremium = false;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

