package com.resumeai.ai.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AtsAnalysisResponse {
    private Integer resumeId;
    private Integer atsScore;
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private String recommendation;
    private List<String> suggestions;
    private String summary;
}

