package com.resumeai.export.controller;

import com.resumeai.export.dto.request.ExportResumeRequest;
import com.resumeai.export.dto.response.ApiResponse;
import com.resumeai.export.dto.response.ExportHistoryResponse;
import com.resumeai.export.dto.response.ExportJobResponse;
import com.resumeai.export.security.JwtAuthenticationFilter;
import com.resumeai.export.service.ExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExportJobResponse>> exportResume(
            @Valid @RequestBody ExportResumeRequest request,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Resume exported successfully",
                        exportService.exportResume(userId, request, authorizationHeader)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExportHistoryResponse>>> getExportHistory(
            HttpServletRequest httpRequest) {
        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Export history retrieved successfully",
                exportService.getExportHistory(userId)));
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<ApiResponse<List<ExportHistoryResponse>>> getExportHistoryByResume(
            @PathVariable Integer resumeId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Resume export history retrieved successfully",
                exportService.getExportHistoryByResume(userId, resumeId)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ExportHistoryResponse>>> getExportHistoryByUser(
            @PathVariable Integer userId,
            HttpServletRequest httpRequest) {

        Integer authenticatedUserId = extractUserId(httpRequest);
        Object roleAttribute = httpRequest.getAttribute(JwtAuthenticationFilter.USER_ROLE_ATTRIBUTE);
        String role = roleAttribute == null ? "" : roleAttribute.toString();
        if (!"ADMIN".equalsIgnoreCase(role) && !authenticatedUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                "Export history retrieved successfully",
                exportService.getExportHistory(userId)));
    }

    @GetMapping("/{exportId}")
    public ResponseEntity<ApiResponse<ExportJobResponse>> getExportById(
            @PathVariable Integer exportId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Export job retrieved successfully",
                exportService.getExportById(exportId, userId)));
    }

    private Integer extractUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request attributes");
        }
        return (Integer) userId;
    }
}
