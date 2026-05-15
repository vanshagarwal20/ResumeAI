package com.resumeai.ai.dto.response;

import com.resumeai.ai.entity.AiRequest.RequestStatus;
import com.resumeai.ai.entity.AiRequest.RequestType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiRequestResponse {
    private Integer aiRequestId;
    private RequestType requestType;
    private String modelUsed;
    private Integer tokenCount;
    private RequestStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
