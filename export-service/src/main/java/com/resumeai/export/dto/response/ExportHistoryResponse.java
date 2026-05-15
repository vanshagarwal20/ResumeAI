package com.resumeai.export.dto.response;

import com.resumeai.export.entity.ExportJob.ExportFormat;
import com.resumeai.export.entity.ExportJob.ExportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExportHistoryResponse {
    private Integer exportId;
    private Integer userId;
    private Integer resumeId;
    private Integer templateId;
    private ExportFormat exportFormat;
    private ExportStatus status;
    private String fileName;
    private String downloadUrl;
    private LocalDateTime createdAt;
    // generatedHtml excluded from list view intentionally
}