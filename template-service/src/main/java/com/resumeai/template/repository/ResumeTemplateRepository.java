package com.resumeai.template.repository;

import com.resumeai.template.entity.ResumeTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeTemplateRepository extends JpaRepository<ResumeTemplate, Integer> {

    List<ResumeTemplate> findByIsPublicTrueAndIsActiveTrueOrderByCreatedAtDesc();

    List<ResumeTemplate> findByIsActiveTrueOrderByCreatedAtDesc();
}

