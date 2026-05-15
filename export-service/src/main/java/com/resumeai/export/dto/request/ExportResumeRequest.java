package com.resumeai.export.dto.request;

import com.resumeai.export.entity.ExportJob.ExportFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExportResumeRequest {

    @NotNull(message = "resumeId is required")
    private Integer resumeId;

    @NotNull(message = "templateId is required")
    private Integer templateId;

    @NotNull(message = "exportFormat is required")
    private ExportFormat exportFormat;
}
