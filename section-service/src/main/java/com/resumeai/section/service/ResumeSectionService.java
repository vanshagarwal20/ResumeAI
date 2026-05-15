package com.resumeai.section.service;

import com.resumeai.section.dto.request.CreateResumeSectionRequest;
import com.resumeai.section.dto.request.ReorderSectionsRequest;
import com.resumeai.section.dto.request.UpdateResumeSectionRequest;
import com.resumeai.section.dto.response.ResumeSectionResponse;

import java.util.List;

public interface ResumeSectionService {

    ResumeSectionResponse createSection(Integer userId, CreateResumeSectionRequest request);

    List<ResumeSectionResponse> getSectionsByResume(Integer resumeId, Integer userId);

    ResumeSectionResponse getSectionById(Integer sectionId, Integer userId);

    ResumeSectionResponse updateSection(Integer sectionId, Integer userId, UpdateResumeSectionRequest request);

    void deleteSection(Integer sectionId, Integer userId);

    List<ResumeSectionResponse> reorderSections(Integer resumeId, Integer userId, ReorderSectionsRequest request);

    List<ResumeSectionResponse> duplicateSections(Integer sourceResumeId, Integer targetResumeId, Integer userId);

    void deleteSectionsByResume(Integer resumeId, Integer userId);
}

