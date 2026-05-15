package com.resumeai.section.controller;


import com.resumeai.section.dto.request.CreateResumeSectionRequest;
import com.resumeai.section.dto.request.ReorderSectionsRequest;
import com.resumeai.section.dto.request.UpdateResumeSectionRequest;
import com.resumeai.section.dto.response.ApiResponse;
import com.resumeai.section.dto.response.ResumeSectionResponse;
import com.resumeai.section.security.JwtAuthenticationFilter;
import com.resumeai.section.service.ResumeSectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
public class ResumeSectionController {

    private final ResumeSectionService sectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<ResumeSectionResponse>> createSection(
            @Valid @RequestBody CreateResumeSectionRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        ResumeSectionResponse response = sectionService.createSection(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Section created successfully", response));
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<ApiResponse<List<ResumeSectionResponse>>> getSectionsByResume(
            @PathVariable Integer resumeId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Sections retrieved successfully",
                sectionService.getSectionsByResume(resumeId, userId)));
    }

    @GetMapping("/{sectionId}")
    public ResponseEntity<ApiResponse<ResumeSectionResponse>> getSectionById(
            @PathVariable Integer sectionId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Section retrieved successfully",
                sectionService.getSectionById(sectionId, userId)));
    }

    @PutMapping("/{sectionId}")
    public ResponseEntity<ApiResponse<ResumeSectionResponse>> updateSection(
            @PathVariable Integer sectionId,
            @Valid @RequestBody UpdateResumeSectionRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Section updated successfully",
                sectionService.updateSection(sectionId, userId, request)));
    }

    @DeleteMapping("/{sectionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable Integer sectionId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        sectionService.deleteSection(sectionId, userId);
        return ResponseEntity.ok(ApiResponse.success("Section deleted successfully"));
    }

    @PutMapping("/resume/{resumeId}/reorder")
    public ResponseEntity<ApiResponse<List<ResumeSectionResponse>>> reorderSections(
            @PathVariable Integer resumeId,
            @Valid @RequestBody ReorderSectionsRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                "Sections reordered successfully",
                sectionService.reorderSections(resumeId, userId, request)));
    }

    @PostMapping("/resume/{sourceResumeId}/duplicate/{targetResumeId}")
    public ResponseEntity<ApiResponse<List<ResumeSectionResponse>>> duplicateSections(
            @PathVariable Integer sourceResumeId,
            @PathVariable Integer targetResumeId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Sections duplicated successfully",
                        sectionService.duplicateSections(sourceResumeId, targetResumeId, userId)));
    }

    @DeleteMapping("/resume/{resumeId}/all")
    public ResponseEntity<ApiResponse<Void>> deleteSectionsByResume(
            @PathVariable Integer resumeId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);
        sectionService.deleteSectionsByResume(resumeId, userId);
        return ResponseEntity.ok(ApiResponse.success("All resume sections deleted successfully"));
    }

    private Integer extractUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request attributes");
        }
        return (Integer) userId;
    }
}

