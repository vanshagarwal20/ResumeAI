package com.resumeai.export.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.export.dto.request.ExportResumeRequest;
import com.resumeai.export.dto.response.ExportHistoryResponse;
import com.resumeai.export.dto.response.ExportJobResponse;
import com.resumeai.export.entity.ExportJob.ExportFormat;
import com.resumeai.export.entity.ExportJob.ExportStatus;
import com.resumeai.export.security.JwtAuthenticationFilter;
import com.resumeai.export.service.ExportService;
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
class ExportControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController controller;

    private static final Integer USER_ID = 1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private ExportJobResponse buildExportJobResponse(Integer id) {
        return ExportJobResponse.builder()
                .exportId(id)
                .userId(USER_ID)
                .resumeId(10)
                .templateId(1)
                .exportFormat(ExportFormat.PDF)
                .status(ExportStatus.COMPLETED)
                .fileName("Software_Engineer_Resume.pdf")
                .downloadUrl("http://localhost:8086/downloads/Software_Engineer_Resume.pdf")
                .generatedHtml("<html><body>Resume</body></html>")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ExportHistoryResponse buildExportHistoryResponse(Integer id) {
        return ExportHistoryResponse.builder()
                .exportId(id)
                .userId(USER_ID)
                .resumeId(10)
                .templateId(1)
                .exportFormat(ExportFormat.PDF)
                .status(ExportStatus.COMPLETED)
                .fileName("Software_Engineer_Resume.pdf")
                .downloadUrl("http://localhost:8086/downloads/Software_Engineer_Resume.pdf")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/exports — should export resume and return 201")
    void exportResume_shouldReturn201() throws Exception {
        ExportResumeRequest request = new ExportResumeRequest();
        request.setResumeId(10);
        request.setTemplateId(1);
        request.setExportFormat(ExportFormat.PDF);

        when(exportService.exportResume(eq(USER_ID), any(ExportResumeRequest.class), anyString()))
                .thenReturn(buildExportJobResponse(1));

        mockMvc.perform(post("/api/v1/exports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer test-token")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exportId").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(exportService).exportResume(eq(USER_ID), any(ExportResumeRequest.class), anyString());
    }

    @Test
    @DisplayName("GET /api/v1/exports — should return export history")
    void getExportHistory_shouldReturnList() throws Exception {
        when(exportService.getExportHistory(USER_ID))
                .thenReturn(List.of(buildExportHistoryResponse(1), buildExportHistoryResponse(2)));

        mockMvc.perform(get("/api/v1/exports")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(exportService).getExportHistory(USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/exports/resume/{resumeId} — should return history for resume")
    void getExportHistoryByResume_shouldReturnList() throws Exception {
        when(exportService.getExportHistoryByResume(USER_ID, 10))
                .thenReturn(List.of(buildExportHistoryResponse(1)));

        mockMvc.perform(get("/api/v1/exports/resume/10")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(exportService).getExportHistoryByResume(USER_ID, 10);
    }

    @Test
    @DisplayName("GET /api/v1/exports/user/{userId} — own user should succeed")
    void getExportHistoryByUser_ownUser_shouldReturnList() throws Exception {
        when(exportService.getExportHistory(USER_ID))
                .thenReturn(List.of(buildExportHistoryResponse(1)));

        mockMvc.perform(get("/api/v1/exports/user/" + USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/exports/user/{userId} — different user non-admin should get 403")
    void getExportHistoryByUser_differentUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/exports/user/999")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/exports/user/{userId} — ADMIN should access any user")
    void getExportHistoryByUser_admin_shouldSucceed() throws Exception {
        when(exportService.getExportHistory(999))
                .thenReturn(List.of(buildExportHistoryResponse(1)));

        mockMvc.perform(get("/api/v1/exports/user/999")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID)
                        .requestAttr(JwtAuthenticationFilter.USER_ROLE_ATTRIBUTE, "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/exports/{exportId} — should return export by id")
    void getExportById_shouldReturnExport() throws Exception {
        when(exportService.getExportById(1, USER_ID)).thenReturn(buildExportJobResponse(1));

        mockMvc.perform(get("/api/v1/exports/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exportId").value(1));

        verify(exportService).getExportById(1, USER_ID);
    }
}
