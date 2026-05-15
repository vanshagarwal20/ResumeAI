package com.resumeai.notification.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Integer notificationId;
    private Integer userId;
    private String type;
    private String title;
    private String message;
    private Integer referenceId;
    private String referenceType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

