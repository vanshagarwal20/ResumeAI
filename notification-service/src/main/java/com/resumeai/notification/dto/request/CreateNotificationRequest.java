package com.resumeai.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNotificationRequest {

    @NotBlank(message = "type is required")
    @Size(max = 50)
    private String type;

    @NotBlank(message = "title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 1000)
    private String message;

    private Integer referenceId;

    @Size(max = 100)
    private String referenceType;
}

