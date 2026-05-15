package com.resumeai.section.service.impl;

import com.resumeai.section.dto.request.CreateResumeSectionRequest;
import com.resumeai.section.dto.request.UpdateResumeSectionRequest;
import com.resumeai.section.dto.response.ResumeSectionResponse;
import com.resumeai.section.entity.ResumeSection;
import com.resumeai.section.entity.ResumeSection.SectionType;
import com.resumeai.section.exception.ResourceNotFoundException;
import com.resumeai.section.repository.ResumeSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeSectionServiceImplTest {

    @Mock
    private ResumeSectionRepository sectionRepository;

    @InjectMocks
    private ResumeSectionServiceImpl sectionService;

    private static final Integer USER_ID = 1;
    private static final Integer RESUME_ID = 10;
    private ResumeSection sampleSection;

    @BeforeEach
    void setUp() {
        sampleSection = ResumeSection.builder()
                .sectionId(1)
                .resumeId(RESUME_ID)
                .userId(USER_ID)
                .sectionType(SectionType.EXPERIENCE)
                .title("Work Experience")
                .content("Worked at XYZ Corp")
                .displayOrder(1)
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createSection — should set displayOrder if null and save")
    void createSection_autoOrder() {
        CreateResumeSectionRequest request = new CreateResumeSectionRequest();
        request.setResumeId(RESUME_ID);
        request.setSectionType(SectionType.SKILLS);
        request.setTitle("Skills");
        request.setContent("Java, Spring Boot");
        request.setIsVisible(true);

        when(sectionRepository.countByResumeIdAndUserId(RESUME_ID, USER_ID)).thenReturn(2L);
        when(sectionRepository.save(any(ResumeSection.class))).thenReturn(sampleSection);

        ResumeSectionResponse result = sectionService.createSection(USER_ID, request);

        assertThat(result).isNotNull();
        assertThat(result.getSectionId()).isEqualTo(1);
        verify(sectionRepository).save(any(ResumeSection.class));
    }

    @Test
    @DisplayName("getSectionsByResume — should return ordered list")
    void getSectionsByResume_shouldReturnList() {
        when(sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAscCreatedAtAsc(RESUME_ID, USER_ID))
                .thenReturn(List.of(sampleSection));

        List<ResumeSectionResponse> result = sectionService.getSectionsByResume(RESUME_ID, USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Work Experience");
    }

    @Test
    @DisplayName("getSectionById — should return section when owned")
    void getSectionById_shouldReturnWhenOwned() {
        when(sectionRepository.findBySectionIdAndUserId(1, USER_ID)).thenReturn(Optional.of(sampleSection));

        ResumeSectionResponse result = sectionService.getSectionById(1, USER_ID);

        assertThat(result.getSectionId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getSectionById — should throw when not found")
    void getSectionById_shouldThrowWhenNotFound() {
        when(sectionRepository.findBySectionIdAndUserId(999, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.getSectionById(999, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateSection — should update non-null fields only")
    void updateSection_shouldUpdateFields() {
        UpdateResumeSectionRequest request = new UpdateResumeSectionRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated Content");

        when(sectionRepository.findBySectionIdAndUserId(1, USER_ID)).thenReturn(Optional.of(sampleSection));
        when(sectionRepository.save(any(ResumeSection.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeSectionResponse result = sectionService.updateSection(1, USER_ID, request);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getContent()).isEqualTo("Updated Content");
    }

    @Test
    @DisplayName("deleteSection — should delete owned section")
    void deleteSection_shouldDelete() {
        when(sectionRepository.findBySectionIdAndUserId(1, USER_ID)).thenReturn(Optional.of(sampleSection));

        sectionService.deleteSection(1, USER_ID);

        verify(sectionRepository).delete(sampleSection);
    }

    @Test
    @DisplayName("duplicateSections — should copy all sections to new resume")
    void duplicateSections_shouldCopyAll() {
        when(sectionRepository.findByResumeIdAndUserIdOrderByDisplayOrderAscCreatedAtAsc(RESUME_ID, USER_ID))
                .thenReturn(List.of(sampleSection));
        when(sectionRepository.saveAll(anyList())).thenReturn(List.of(sampleSection));

        List<ResumeSectionResponse> result = sectionService.duplicateSections(RESUME_ID, 20, USER_ID);

        assertThat(result).hasSize(1);
        verify(sectionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("deleteSectionsByResume — should delete all for resume and user")
    void deleteSectionsByResume_shouldDeleteAll() {
        sectionService.deleteSectionsByResume(RESUME_ID, USER_ID);

        verify(sectionRepository).deleteByResumeIdAndUserId(RESUME_ID, USER_ID);
    }
}
