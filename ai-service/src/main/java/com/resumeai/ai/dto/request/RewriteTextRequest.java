package com.resumeai.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RewriteTextRequest {

    @NotBlank(message = "content is required")
    @Size(max = 20000)
    private String content;

    @Size(max = 120)
    private String tone;
}
