package com.resumeai.resumeservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.resumeservice.dto.request.AtsScoreUpdateRequest;
import com.resumeai.resumeservice.dto.request.CreateResumeRequest;
import com.resumeai.resumeservice.dto.request.UpdateResumeRequest;
import com.resumeai.resumeservice.dto.response.ResumeResponse;
import com.resumeai.resumeservice.entity.Resume.ResumeStatus;
import com.resumeai.resumeservice.security.JwtAuthenticationFilter;
import com.resumeai.resumeservice.service.ResumeService;
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
class ResumeControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ResumeService resumeService;

    @InjectMocks
    private ResumeController controller;

    private static final Integer USER_ID = 1;
    private static final String PLAN = "FREE";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private ResumeResponse buildResumeResponse(Integer id) {
        return ResumeResponse.builder()
                .resumeId(id)
                .userId(USER_ID)
                .title("Software Engineer Resume")
                .targetJobTitle("Software Engineer")
                .templateId(1)
                .atsScore(0)
                .status(ResumeStatus.DRAFT)
                .language("en")
                .isPublic(false)
                .viewCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/resumes — should create resume")
    void createResume_shouldReturn201() throws Exception {
        CreateResumeRequest request = CreateResumeRequest.builder()
                .title("Software Engineer Resume")
                .targetJobTitle("Software Engineer")
                .templateId(1)
                .build();

        when(resumeService.createResume(eq(USER_ID), eq(PLAN), any(CreateResumeRequest.class)))
                .thenReturn(buildResumeResponse(1));

        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resumeId").value(1));

        verify(resumeService).createResume(eq(USER_ID), eq(PLAN), any(CreateResumeRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/resumes — should return user's resumes")
    void getMyResumes_shouldReturnList() throws Exception {
        when(resumeService.getResumesByUser(USER_ID))
                .thenReturn(List.of(buildResumeResponse(1), buildResumeResponse(2)));

        mockMvc.perform(get("/api/v1/resumes")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(resumeService).getResumesByUser(USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/resumes/{id} — should return single resume")
    void getResumeById_shouldReturnResume() throws Exception {
        when(resumeService.getResumeById(1, USER_ID)).thenReturn(buildResumeResponse(1));

        mockMvc.perform(get("/api/v1/resumes/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/resumes/{id} — should update resume")
    void updateResume_shouldReturnUpdated() throws Exception {
        UpdateResumeRequest request = UpdateResumeRequest.builder()
                .title("Updated Title")
                .build();

        when(resumeService.updateResume(eq(1), eq(USER_ID), any(UpdateResumeRequest.class)))
                .thenReturn(buildResumeResponse(1));

        mockMvc.perform(put("/api/v1/resumes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/resumes/{id} — should delete resume")
    void deleteResume_shouldReturn200() throws Exception {
        doNothing().when(resumeService).deleteResume(eq(1), eq(USER_ID), anyString());

        mockMvc.perform(delete("/api/v1/resumes/1")
                        .header("Authorization", "Bearer test-token")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/resumes/{id}/duplicate — should duplicate resume")
    void duplicateResume_shouldReturn201() throws Exception {
        when(resumeService.duplicateResume(eq(1), eq(USER_ID), eq(PLAN), anyString()))
                .thenReturn(buildResumeResponse(2));

        mockMvc.perform(post("/api/v1/resumes/1/duplicate")
                        .header("Authorization", "Bearer test-token")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_PLAN_ATTRIBUTE, PLAN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.resumeId").value(2));
    }

    @Test
    @DisplayName("PUT /api/v1/resumes/{id}/ats-score — should update ATS score")
    void updateAtsScore_shouldReturn200() throws Exception {
        AtsScoreUpdateRequest request = AtsScoreUpdateRequest.builder().atsScore(85).build();
        doNothing().when(resumeService).updateAtsScore(eq(1), any(AtsScoreUpdateRequest.class));

        mockMvc.perform(put("/api/v1/resumes/1/ats-score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/resumes/public — should return public resumes")
    void getPublicResumes_shouldReturnList() throws Exception {
        when(resumeService.getPublicResumes()).thenReturn(List.of(buildResumeResponse(1)));

        mockMvc.perform(get("/api/v1/resumes/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/resumes/recent — should return most recent resume")
    void getMostRecentResume_shouldReturnResume() throws Exception {
        when(resumeService.getMostRecentResume(USER_ID)).thenReturn(buildResumeResponse(1));

        mockMvc.perform(get("/api/v1/resumes/recent")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumeId").value(1));
    }
}
