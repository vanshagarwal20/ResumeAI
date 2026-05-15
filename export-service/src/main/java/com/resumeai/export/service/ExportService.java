package com.resumeai.export.service;

import com.resumeai.export.dto.request.ExportResumeRequest;
import com.resumeai.export.dto.response.ExportHistoryResponse;
import com.resumeai.export.dto.response.ExportJobResponse;

import java.util.List;

public interface ExportService {

    ExportJobResponse exportResume(Integer userId, ExportResumeRequest request, String authorizationHeader);

    List<ExportHistoryResponse> getExportHistory(Integer userId);

    List<ExportHistoryResponse> getExportHistoryByResume(Integer userId, Integer resumeId);

    ExportJobResponse getExportById(Integer exportId, Integer userId);
}
