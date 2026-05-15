package com.resumeai.section.repository;

import com.resumeai.section.entity.ResumeSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeSectionRepository extends JpaRepository<ResumeSection, Integer> {

    List<ResumeSection> findByResumeIdAndUserIdOrderByDisplayOrderAscCreatedAtAsc(Integer resumeId, Integer userId);

    List<ResumeSection> findByResumeIdAndUserId(Integer resumeId, Integer userId);

    Optional<ResumeSection> findBySectionIdAndUserId(Integer sectionId, Integer userId);

    long countByResumeIdAndUserId(Integer resumeId, Integer userId);

    void deleteByResumeIdAndUserId(Integer resumeId, Integer userId);
}

