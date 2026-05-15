package com.resumeai.export.service.impl;

import com.resumeai.export.dto.request.ExportResumeRequest;
import com.resumeai.export.dto.response.ExportHistoryResponse;
import com.resumeai.export.dto.response.ExportJobResponse;
import com.resumeai.export.dto.response.ResumeDataResponse;
import com.resumeai.export.dto.response.SectionDataResponse;
import com.resumeai.export.dto.response.TemplateDataResponse;
import com.resumeai.export.entity.ExportJob;
import com.resumeai.export.exception.ResourceNotFoundException;
import com.resumeai.export.repository.ExportJobRepository;
import com.resumeai.export.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final ExportJobRepository exportJobRepository;
    private final RestTemplate restTemplate;

    @Value("${services.resume-service-url}")
    private String resumeServiceUrl;

    @Value("${services.section-service-url}")
    private String sectionServiceUrl;

    @Value("${services.template-service-url}")
    private String templateServiceUrl;

    @Override
    @Transactional
    public ExportJobResponse exportResume(Integer userId, ExportResumeRequest request, String authorizationHeader) {
        ResumeDataResponse resume = fetchResume(request.getResumeId(), authorizationHeader);
        List<SectionDataResponse> sections = fetchSections(request.getResumeId(), authorizationHeader);
        TemplateDataResponse template = fetchTemplate(request.getTemplateId(), authorizationHeader);

        String exportContent = buildExportContent(resume, sections, template, request.getExportFormat());
        String fileName    = resume.getTitle().replaceAll("\\s+", "_") + "." + request.getExportFormat().name().toLowerCase();
        String downloadUrl = "http://localhost:8086/downloads/" + fileName;

        ExportJob exportJob = ExportJob.builder()
                .userId(userId)
                .resumeId(request.getResumeId())
                .templateId(request.getTemplateId())
                .exportFormat(request.getExportFormat())
                .status(ExportJob.ExportStatus.COMPLETED)
                .fileName(fileName)
                .downloadUrl(downloadUrl)
                .generatedHtml(exportContent)
                .build();

        return mapToFullResponse(exportJobRepository.save(exportJob));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportHistoryResponse> getExportHistory(Integer userId) {
        return exportJobRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExportHistoryResponse> getExportHistoryByResume(Integer userId, Integer resumeId) {
        return exportJobRepository.findByUserIdAndResumeIdOrderByCreatedAtDesc(userId, resumeId)
                .stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobResponse getExportById(Integer exportId, Integer userId) {
        ExportJob exportJob = exportJobRepository.findById(exportId)
                .orElseThrow(() -> new ResourceNotFoundException("ExportJob", "exportId", exportId));

        if (!exportJob.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("ExportJob", "exportId", exportId);
        }

        return mapToFullResponse(exportJob);
    }

    private String buildExportContent(ResumeDataResponse resume,
                                      List<SectionDataResponse> sections,
                                      TemplateDataResponse template,
                                      ExportJob.ExportFormat exportFormat) {
        if (exportFormat == ExportJob.ExportFormat.JSON) {
            return buildJsonExport(resume, sections, template);
        }
        if (exportFormat == ExportJob.ExportFormat.DOCX) {
            return buildDocxTextExport(resume, sections);
        }
                                  
        int templateId = template != null && template.getTemplateId() != null ? template.getTemplateId() : 1;
        
        switch (templateId) {
            case 2:
                return buildProfessionalHtml(resume, sections);
            case 3:
                return buildCreativeHtml(resume, sections);
            case 4:
                return buildMinimalistHtml(resume, sections);
            case 1:
            default:
                return buildModernHtml(resume, sections);
        }
    }

    private String buildJsonExport(ResumeDataResponse resume,
                                   List<SectionDataResponse> sections,
                                   TemplateDataResponse template) {
        StringBuilder json = new StringBuilder();
        json.append("{")
            .append("\"resumeId\":").append(resume.getResumeId()).append(",")
            .append("\"title\":\"").append(escapeJson(resume.getTitle())).append("\",")
            .append("\"targetJobTitle\":\"").append(escapeJson(resume.getTargetJobTitle())).append("\",")
            .append("\"templateId\":").append(template == null ? "null" : template.getTemplateId()).append(",")
            .append("\"language\":\"").append(escapeJson(resume.getLanguage())).append("\",")
            .append("\"sections\":[");
        for (int i = 0; i < sections.size(); i++) {
            SectionDataResponse section = sections.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{")
                .append("\"sectionId\":").append(section.getSectionId()).append(",")
                .append("\"sectionType\":\"").append(escapeJson(section.getSectionType())).append("\",")
                .append("\"title\":\"").append(escapeJson(section.getTitle())).append("\",")
                .append("\"content\":\"").append(escapeJson(section.getContent())).append("\",")
                .append("\"displayOrder\":").append(section.getDisplayOrder()).append(",")
                .append("\"visible\":").append(Boolean.TRUE.equals(section.getIsVisible()))
                .append("}");
        }
        json.append("]}");
        return json.toString();
    }

    private String buildDocxTextExport(ResumeDataResponse resume, List<SectionDataResponse> sections) {
        StringBuilder document = new StringBuilder();
        document.append(resume.getTitle()).append("\n");
        if (resume.getTargetJobTitle() != null) {
            document.append(resume.getTargetJobTitle()).append("\n");
        }
        document.append("\n");
        for (SectionDataResponse section : sections) {
            if (Boolean.FALSE.equals(section.getIsVisible())) {
                continue;
            }
            document.append(section.getTitle()).append("\n")
                    .append(section.getContent()).append("\n\n");
        }
        return document.toString();
    }

    private String buildModernHtml(ResumeDataResponse resume, List<SectionDataResponse> sections) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: sans-serif; padding: 40px;'>");
        html.append("<h1 style='text-transform: uppercase; color: #4f46e5;'>").append(escapeHtml(resume.getTitle())).append("</h1>");
        if (resume.getTargetJobTitle() != null) {
            html.append("<p style='color: #6b7280; font-weight: bold;'>").append(escapeHtml(resume.getTargetJobTitle())).append("</p>");
        }
        html.append("<hr style='border: 2px solid #4f46e5; margin-bottom: 20px;'/>");
        
        for (SectionDataResponse section : sections) {
            if (Boolean.FALSE.equals(section.getIsVisible())) continue;
            html.append("<div style='margin-bottom: 20px;'>")
                .append("<h3 style='color: #4f46e5; text-transform: uppercase; font-size: 14px;'>").append(escapeHtml(section.getTitle())).append("</h3>")
                .append("<p style='font-size: 14px;'>").append(escapeHtml(section.getContent()).replace("\n", "<br/>")).append("</p>")
                .append("</div>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    private String buildProfessionalHtml(ResumeDataResponse resume, List<SectionDataResponse> sections) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: serif; padding: 40px; text-align: center;'>");
        html.append("<h1 style='font-size: 32px;'>").append(escapeHtml(resume.getTitle())).append("</h1>");
        if (resume.getTargetJobTitle() != null) {
            html.append("<p style='font-style: italic;'>").append(escapeHtml(resume.getTargetJobTitle())).append("</p>");
        }
        html.append("<hr style='border: 1px solid #000; margin-bottom: 20px;'/>");
        html.append("<div style='text-align: left;'>");
        
        for (SectionDataResponse section : sections) {
            if (Boolean.FALSE.equals(section.getIsVisible())) continue;
            html.append("<div style='margin-bottom: 20px;'>")
                .append("<h3 style='border-bottom: 1px solid #ccc; font-size: 16px;'>").append(escapeHtml(section.getTitle())).append("</h3>")
                .append("<p style='font-size: 14px;'>").append(escapeHtml(section.getContent()).replace("\n", "<br/>")).append("</p>")
                .append("</div>");
        }
        html.append("</div></body></html>");
        return html.toString();
    }

    private String buildCreativeHtml(ResumeDataResponse resume, List<SectionDataResponse> sections) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: sans-serif; margin: 0; padding: 0;'>");
        html.append("<div style='background-color: #0d9488; color: white; padding: 40px; text-align: center;'>");
        html.append("<h1 style='font-size: 36px; margin: 0;'>").append(escapeHtml(resume.getTitle())).append("</h1>");
        if (resume.getTargetJobTitle() != null) {
            html.append("<p>").append(escapeHtml(resume.getTargetJobTitle())).append("</p>");
        }
        html.append("</div>");
        html.append("<div style='padding: 40px;'>");
        
        for (SectionDataResponse section : sections) {
            if (Boolean.FALSE.equals(section.getIsVisible())) continue;
            html.append("<div style='margin-bottom: 24px; border-left: 4px solid #0d9488; padding-left: 12px;'>")
                .append("<h3 style='color: #0d9488; margin-top: 0;'>").append(escapeHtml(section.getTitle())).append("</h3>")
                .append("<p style='font-size: 14px; color: #333;'>").append(escapeHtml(section.getContent()).replace("\n", "<br/>")).append("</p>")
                .append("</div>");
        }
        html.append("</div></body></html>");
        return html.toString();
    }

    private String buildMinimalistHtml(ResumeDataResponse resume, List<SectionDataResponse> sections) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: sans-serif; padding: 50px; color: #333;'>");
        html.append("<h1 style='font-weight: 300; letter-spacing: 2px;'>").append(escapeHtml(resume.getTitle())).append("</h1>");
        if (resume.getTargetJobTitle() != null) {
            html.append("<p style='color: #888; text-transform: uppercase; font-size: 12px;'>").append(escapeHtml(resume.getTargetJobTitle())).append("</p>");
        }
        html.append("<br/>");
        
        for (SectionDataResponse section : sections) {
            if (Boolean.FALSE.equals(section.getIsVisible())) continue;
            html.append("<div style='margin-bottom: 25px;'>")
                .append("<h4 style='color: #999; text-transform: uppercase; font-size: 12px; margin-bottom: 5px;'>").append(escapeHtml(section.getTitle())).append("</h4>")
                .append("<p style='font-size: 14px; font-weight: 300; line-height: 1.6;'>").append(escapeHtml(section.getContent()).replace("\n", "<br/>")).append("</p>")
                .append("</div>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\r", "\\r")
                   .replace("\n", "\\n");
    }

    private ResumeDataResponse fetchResume(Integer resumeId, String authorizationHeader) {
        String url = resumeServiceUrl + "/api/v1/resumes/" + resumeId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        ResumeDataResponse resume = new ResumeDataResponse();
        resume.setResumeId((Integer) data.get("resumeId"));
        resume.setUserId((Integer) data.get("userId"));
        resume.setTitle((String) data.get("title"));
        resume.setTargetJobTitle((String) data.get("targetJobTitle"));
        resume.setTemplateId((Integer) data.get("templateId"));
        resume.setAtsScore((Integer) data.get("atsScore"));
        resume.setStatus(String.valueOf(data.get("status")));
        resume.setLanguage((String) data.get("language"));
        resume.setIsPublic((Boolean) data.get("isPublic"));
        resume.setViewCount((Integer) data.get("viewCount"));
        return resume;
    }

    private List<SectionDataResponse> fetchSections(Integer resumeId, String authorizationHeader) {
        String url = sectionServiceUrl + "/api/v1/sections/resume/" + resumeId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

        return data.stream().map(item -> {
            SectionDataResponse section = new SectionDataResponse();
            section.setSectionId((Integer) item.get("sectionId"));
            section.setResumeId((Integer) item.get("resumeId"));
            section.setUserId((Integer) item.get("userId"));
            section.setSectionType(String.valueOf(item.get("sectionType")));
            section.setTitle((String) item.get("title"));
            section.setContent((String) item.get("content"));
            section.setDisplayOrder((Integer) item.get("displayOrder"));
            section.setIsVisible((Boolean) item.get("isVisible"));
            return section;
        }).toList();
    }

    private TemplateDataResponse fetchTemplate(Integer templateId, String authorizationHeader) {
        String url = templateServiceUrl + "/api/v1/templates/" + templateId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<Map<String, Object>>() {}).getBody();
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        TemplateDataResponse template = new TemplateDataResponse();
        template.setTemplateId((Integer) data.get("templateId"));
        template.setName((String) data.get("name"));
        template.setCategory((String) data.get("category"));
        template.setDescription((String) data.get("description"));
        template.setPreviewImageUrl((String) data.get("previewImageUrl"));
        template.setHtmlStructure((String) data.get("htmlStructure"));
        template.setIsPremium((Boolean) data.get("isPremium"));
        template.setIsPublic((Boolean) data.get("isPublic"));
        template.setIsActive((Boolean) data.get("isActive"));
        return template;
    }

    private ExportJobResponse mapToFullResponse(ExportJob job) {
        return ExportJobResponse.builder()
                .exportId(job.getExportId())
                .userId(job.getUserId())
                .resumeId(job.getResumeId())
                .templateId(job.getTemplateId())
                .exportFormat(job.getExportFormat())
                .status(job.getStatus())
                .fileName(job.getFileName())
                .downloadUrl(job.getDownloadUrl())
                .generatedHtml(job.getGeneratedHtml())
                .createdAt(job.getCreatedAt())
                .build();
    }

    private ExportHistoryResponse mapToHistoryResponse(ExportJob job) {
        return ExportHistoryResponse.builder()
                .exportId(job.getExportId())
                .userId(job.getUserId())
                .resumeId(job.getResumeId())
                .templateId(job.getTemplateId())
                .exportFormat(job.getExportFormat())
                .status(job.getStatus())
                .fileName(job.getFileName())
                .downloadUrl(job.getDownloadUrl())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
