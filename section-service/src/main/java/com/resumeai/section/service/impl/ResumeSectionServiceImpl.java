package com.resumeai.section.service.impl;

import com.resumeai.section.dto.request.CreateResumeSectionRequest;
import com.resumeai.section.dto.request.ReorderSectionsRequest;
import com.resumeai.section.dto.request.UpdateResumeSectionRequest;
import com.resumeai.section.dto.response.ResumeSectionResponse;
import com.resumeai.section.entity.ResumeSection;
import com.resumeai.section.exception.AccessDeniedException;
import com.resumeai.section.exception.ResourceNotFoundException;
import com.resumeai.section.repository.ResumeSectionRepository;
import com.resumeai.section.service.ResumeSectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeSectionServiceImpl implements ResumeSectionService {

    private final ResumeSectionRepository sectionRepository;

    @Override
    @Transactional
    public ResumeSectionResponse createSection(Integer userId, CreateResumeSectionRequest request) {
        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            displayOrder = (int) sectionRepository.countByResumeIdAndUserId(request.getResumeId(), userId) + 1;
        }

        ResumeSection section = ResumeSection.builder()
                .resumeId(request.getResumeId())
                .userId(userId)
                .sectionType(request.getSectionType())
                .title(request.getTitle())
                .content(request.getContent())
                .displayOrder(displayOrder)
                .isVisible(request.getIsVisible() == null ? true : request.getIsVisible())
                .build();

        return toResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeSectionResponse> getSectionsByResume(Integer resumeId, Integer userId) {
        return sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAscCreatedAtAsc(resumeId, userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeSectionResponse getSectionById(Integer sectionId, Integer userId) {
        return toResponse(findOwnedSection(sectionId, userId));
    }

    @Override
    @Transactional
    public ResumeSectionResponse updateSection(Integer sectionId, Integer userId, UpdateResumeSectionRequest request) {
        ResumeSection section = findOwnedSection(sectionId, userId);

        if (request.getSectionType() != null) {
            section.setSectionType(request.getSectionType());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            section.setTitle(request.getTitle());
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            section.setContent(request.getContent());
        }
        if (request.getDisplayOrder() != null) {
            section.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsVisible() != null) {
            section.setIsVisible(request.getIsVisible());
        }

        return toResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public void deleteSection(Integer sectionId, Integer userId) {
        ResumeSection section = findOwnedSection(sectionId, userId);
        sectionRepository.delete(section);
    }

    @Override
    @Transactional
    public List<ResumeSectionResponse> reorderSections(Integer resumeId, Integer userId, ReorderSectionsRequest request) {
        List<ResumeSection> sections = sectionRepository.findByResumeIdAndUserId(resumeId, userId);
        Map<Integer, ResumeSection> sectionMap = new HashMap<>();

        for (ResumeSection section : sections) {
            sectionMap.put(section.getSectionId(), section);
        }

        for (ReorderSectionsRequest.SectionOrderItem item : request.getSections()) {
            ResumeSection section = sectionMap.get(item.getSectionId());
            if (section == null) {
                throw new ResourceNotFoundException("ResumeSection", "sectionId", item.getSectionId());
            }
            if (!section.getUserId().equals(userId)) {
                throw new AccessDeniedException("You do not have permission to reorder this section");
            }
            section.setDisplayOrder(item.getDisplayOrder());
        }

        sectionRepository.saveAll(sections);
        return getSectionsByResume(resumeId, userId);
    }

    @Override
    @Transactional
    public List<ResumeSectionResponse> duplicateSections(Integer sourceResumeId, Integer targetResumeId, Integer userId) {
        List<ResumeSection> sourceSections = sectionRepository
                .findByResumeIdAndUserIdOrderByDisplayOrderAscCreatedAtAsc(sourceResumeId, userId);

        List<ResumeSection> copies = sourceSections.stream()
                .map(section -> ResumeSection.builder()
                        .resumeId(targetResumeId)
                        .userId(userId)
                        .sectionType(section.getSectionType())
                        .title(section.getTitle())
                        .content(section.getContent())
                        .displayOrder(section.getDisplayOrder())
                        .isVisible(section.getIsVisible())
                        .build())
                .toList();

        return sectionRepository.saveAll(copies).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteSectionsByResume(Integer resumeId, Integer userId) {
        sectionRepository.deleteByResumeIdAndUserId(resumeId, userId);
        log.info("Deleted all sections for resume {} and user {}", resumeId, userId);
    }

    private ResumeSection findOwnedSection(Integer sectionId, Integer userId) {
        return sectionRepository.findBySectionIdAndUserId(sectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ResumeSection", "sectionId", sectionId));
    }

    private ResumeSectionResponse toResponse(ResumeSection section) {
        return ResumeSectionResponse.builder()
                .sectionId(section.getSectionId())
                .resumeId(section.getResumeId())
                .userId(section.getUserId())
                .sectionType(section.getSectionType())
                .title(section.getTitle())
                .content(section.getContent())
                .displayOrder(section.getDisplayOrder())
                .isVisible(section.getIsVisible())
                .createdAt(section.getCreatedAt())
                .updatedAt(section.getUpdatedAt())
                .build();
    }
}

