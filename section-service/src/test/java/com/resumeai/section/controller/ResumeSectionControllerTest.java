package com.resumeai.section.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.section.dto.request.CreateResumeSectionRequest;
import com.resumeai.section.dto.request.UpdateResumeSectionRequest;
import com.resumeai.section.dto.response.ResumeSectionResponse;
import com.resumeai.section.entity.ResumeSection.SectionType;
import com.resumeai.section.security.JwtAuthenticationFilter;
import com.resumeai.section.service.ResumeSectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ResumeSectionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ResumeSectionService sectionService;

    @InjectMocks
    private ResumeSectionController controller;

    private static final Integer USER_ID = 1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private ResumeSectionResponse buildSectionResponse(Integer id) {
        return ResumeSectionResponse.builder()
                .sectionId(id)
                .resumeId(10)
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
    @DisplayName("POST /api/v1/sections — should create section")
    void createSection_shouldReturn201() throws Exception {
        CreateResumeSectionRequest request = new CreateResumeSectionRequest();
        request.setResumeId(10);
        request.setSectionType(SectionType.EXPERIENCE);
        request.setTitle("Work Experience");
        request.setContent("Worked at XYZ Corp");
        request.setDisplayOrder(1);
        request.setIsVisible(true);

        when(sectionService.createSection(eq(USER_ID), any(CreateResumeSectionRequest.class)))
                .thenReturn(buildSectionResponse(1));

        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sectionId").value(1));

        verify(sectionService).createSection(eq(USER_ID), any(CreateResumeSectionRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/sections/resume/{resumeId} — should return sections")
    void getSectionsByResume_shouldReturnList() throws Exception {
        when(sectionService.getSectionsByResume(10, USER_ID))
                .thenReturn(List.of(buildSectionResponse(1), buildSectionResponse(2)));

        mockMvc.perform(get("/api/v1/sections/resume/10")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(sectionService).getSectionsByResume(10, USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/sections/{sectionId} — should return single section")
    void getSectionById_shouldReturnSection() throws Exception {
        when(sectionService.getSectionById(1, USER_ID)).thenReturn(buildSectionResponse(1));

        mockMvc.perform(get("/api/v1/sections/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sectionId").value(1));

        verify(sectionService).getSectionById(1, USER_ID);
    }

    @Test
    @DisplayName("PUT /api/v1/sections/{sectionId} — should update section")
    void updateSection_shouldReturnUpdated() throws Exception {
        UpdateResumeSectionRequest request = new UpdateResumeSectionRequest();
        request.setTitle("Updated Title");
        request.setContent("Updated Content");

        when(sectionService.updateSection(eq(1), eq(USER_ID), any(UpdateResumeSectionRequest.class)))
                .thenReturn(buildSectionResponse(1));

        mockMvc.perform(put("/api/v1/sections/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(sectionService).updateSection(eq(1), eq(USER_ID), any(UpdateResumeSectionRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/sections/{sectionId} — should delete section")
    void deleteSection_shouldReturn200() throws Exception {
        doNothing().when(sectionService).deleteSection(1, USER_ID);

        mockMvc.perform(delete("/api/v1/sections/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(sectionService).deleteSection(1, USER_ID);
    }

    @Test
    @DisplayName("DELETE /api/v1/sections/resume/{resumeId}/all — should delete all sections")
    void deleteSectionsByResume_shouldReturn200() throws Exception {
        doNothing().when(sectionService).deleteSectionsByResume(10, USER_ID);

        mockMvc.perform(delete("/api/v1/sections/resume/10/all")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(sectionService).deleteSectionsByResume(10, USER_ID);
    }
}
