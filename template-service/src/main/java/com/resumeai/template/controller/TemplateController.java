package com.resumeai.template.controller;


import com.resumeai.template.dto.request.CreateTemplateRequest;
import com.resumeai.template.dto.request.UpdateTemplateRequest;
import com.resumeai.template.dto.response.ApiResponse;
import com.resumeai.template.dto.response.TemplateResponse;
import com.resumeai.template.dto.response.TemplateUsageResponse;
import com.resumeai.template.security.JwtAuthenticationFilter;
import com.resumeai.template.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    /**
     * Admin creates a new template.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @Valid @RequestBody CreateTemplateRequest request,
            HttpServletRequest httpRequest) {

        ensureAdmin(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created successfully", templateService.createTemplate(request)));
    }

    /**
     * Admin can see all active templates.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getAllTemplates(HttpServletRequest httpRequest) {
        ensureAuthenticated(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Templates retrieved successfully",
                templateService.getAllTemplates()));
    }

    /**
     * Public endpoint used by landing page / template picker.
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getPublicTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean premium) {
        return ResponseEntity.ok(ApiResponse.success(
                "Public templates retrieved successfully",
                templateService.getPublicTemplates(category, premium)));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplateById(
            @PathVariable Integer templateId,
            HttpServletRequest httpRequest) {

        ensureAuthenticated(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Template retrieved successfully",
                templateService.getTemplateById(templateId)));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable Integer templateId,
            @Valid @RequestBody UpdateTemplateRequest request,
            HttpServletRequest httpRequest) {

        ensureAdmin(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Template updated successfully",
                templateService.updateTemplate(templateId, request)));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable Integer templateId,
            HttpServletRequest httpRequest) {

        ensureAdmin(httpRequest);
        templateService.deleteTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("Template deleted successfully"));
    }

    /**
     * Connected endpoint: this service calls resume-service
     * to calculate how many resumes are using this template.
     */
    @GetMapping("/{templateId}/usage")
    public ResponseEntity<ApiResponse<TemplateUsageResponse>> getTemplateUsage(
            @PathVariable Integer templateId,
            HttpServletRequest httpRequest) {

        ensureAuthenticated(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Template usage retrieved successfully",
                templateService.getTemplateUsage(templateId)));
    }

    private void ensureAuthenticated(HttpServletRequest request) {
        Object userId = request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new IllegalStateException("User authentication details not found");
        }
    }

    private void ensureAdmin(HttpServletRequest request) {
        ensureAuthenticated(request);
        Object role = request.getAttribute(JwtAuthenticationFilter.USER_ROLE_ATTRIBUTE);
        if (role == null || !"ADMIN".equals(role.toString())) {
            throw new IllegalStateException("Only ADMIN can perform this operation");
        }
    }
}

