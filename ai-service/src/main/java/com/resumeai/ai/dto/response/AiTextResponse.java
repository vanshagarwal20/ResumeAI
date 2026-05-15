package com.resumeai.ai.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTextResponse {
    private String generatedText;
    private String modelUsed;
    private Integer tokenCount;
}

 
