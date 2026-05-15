package com.resumeai.ai.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTextResponse {
    private String generatedText;
}

