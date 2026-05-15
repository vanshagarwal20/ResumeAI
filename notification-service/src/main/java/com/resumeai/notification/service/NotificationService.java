package com.resumeai.notification.service;

import com.resumeai.notification.dto.request.CreateNotificationRequest;
import com.resumeai.notification.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(Integer userId, CreateNotificationRequest request);

    List<NotificationResponse> getMyNotifications(Integer userId);

    long getUnreadCount(Integer userId);

    NotificationResponse markAsRead(Integer userId, Integer notificationId);

    void markAllAsRead(Integer userId);
}

