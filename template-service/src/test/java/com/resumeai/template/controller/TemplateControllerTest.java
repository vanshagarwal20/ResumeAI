package com.resumeai.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.template.dto.request.CreateTemplateRequest;
import com.resumeai.template.dto.request.UpdateTemplateRequest;
import com.resumeai.template.dto.response.TemplateResponse;
import com.resumeai.template.dto.response.TemplateUsageResponse;
import com.resumeai.template.security.JwtAuthenticationFilter;
import com.resumeai.template.service.TemplateService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TemplateControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private TemplateController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private TemplateResponse buildTemplateResponse(Integer id) {
        return TemplateResponse.builder()
                .templateId(id)
                .name("Modern Template")
                .category("Professional")
                .description("A modern resume template")
                .previewImageUrl("http://example.com/preview.png")
                .htmlStructure("<html></html>")
                .isPremium(false)
                .isPublic(true)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/templates — admin should create template")
    void createTemplate_shouldReturn201() throws Exception {
        CreateTemplateRequest request = new CreateTemplateRequest();
        request.setName("Modern Template");
        request.setCategory("Professional");
        request.setDescription("A modern resume template");
        request.setPreviewImageUrl("http://example.com/preview.png");
        request.setHtmlStructure("<html></html>");
        request.setIsPremium(false);

        when(templateService.createTemplate(any(CreateTemplateRequest.class)))
                .thenReturn(buildTemplateResponse(1));

        mockMvc.perform(post("/api/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, 1)
                        .requestAttr(JwtAuthenticationFilter.USER_ROLE_ATTRIBUTE, "ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.templateId").value(1));

        verify(templateService).createTemplate(any(CreateTemplateRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/templates — authenticated user should get all templates")
    void getAllTemplates_shouldReturnList() throws Exception {
        when(templateService.getAllTemplates()).thenReturn(List.of(buildTemplateResponse(1)));

        mockMvc.perform(get("/api/v1/templates")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(templateService).getAllTemplates();
    }

    @Test
    @DisplayName("GET /api/v1/templates/public — should return public templates")
    void getPublicTemplates_shouldReturnList() throws Exception {
        when(templateService.getPublicTemplates(isNull(), isNull()))
                .thenReturn(List.of(buildTemplateResponse(1)));

        mockMvc.perform(get("/api/v1/templates/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(templateService).getPublicTemplates(isNull(), isNull());
    }

    @Test
    @DisplayName("GET /api/v1/templates/{templateId} — should return template by id")
    void getTemplateById_shouldReturnTemplate() throws Exception {
        when(templateService.getTemplateById(1)).thenReturn(buildTemplateResponse(1));

        mockMvc.perform(get("/api/v1/templates/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(1));

        verify(templateService).getTemplateById(1);
    }

    @Test
    @DisplayName("PUT /api/v1/templates/{templateId} — admin should update template")
    void updateTemplate_shouldReturnUpdated() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest();
        request.setName("Updated Template");

        when(templateService.updateTemplate(eq(1), any(UpdateTemplateRequest.class)))
                .thenReturn(buildTemplateResponse(1));

        mockMvc.perform(put("/api/v1/templates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, 1)
                        .requestAttr(JwtAuthenticationFilter.USER_ROLE_ATTRIBUTE, "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(templateService).updateTemplate(eq(1), any(UpdateTemplateRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/templates/{templateId} — admin should delete template")
    void deleteTemplate_shouldReturn200() throws Exception {
        doNothing().when(templateService).deleteTemplate(1);

        mockMvc.perform(delete("/api/v1/templates/1")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, 1)
                        .requestAttr(JwtAuthenticationFilter.USER_ROLE_ATTRIBUTE, "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(templateService).deleteTemplate(1);
    }

    @Test
    @DisplayName("GET /api/v1/templates/{templateId}/usage — should return usage stats")
    void getTemplateUsage_shouldReturnUsage() throws Exception {
        TemplateUsageResponse usage = TemplateUsageResponse.builder()
                .templateId(1)
                .usageCount(5)
                .resumes(Collections.emptyList())
                .build();

        when(templateService.getTemplateUsage(1)).thenReturn(usage);

        mockMvc.perform(get("/api/v1/templates/1/usage")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usageCount").value(5));

        verify(templateService).getTemplateUsage(1);
    }
}
