package com.resumeai.notification.controller;

import com.resumeai.notification.dto.request.CreateNotificationRequest;
import com.resumeai.notification.dto.response.ApiResponse;
import com.resumeai.notification.dto.response.NotificationResponse;
import com.resumeai.notification.security.JwtAuthenticationFilter;
import com.resumeai.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * Controller flow:
 * - user creates a notification
 * - user gets all notifications
 * - user checks unread count
 * - user marks one notification or all notifications as read
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Notification created successfully",
                        notificationService.createNotification(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "Notifications retrieved successfully",
                notificationService.getMyNotifications(userId)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(HttpServletRequest httpRequest) {
        Integer userId = extractUserId(httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "Unread count retrieved successfully",
                notificationService.getUnreadCount(userId)));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Integer notificationId,
            HttpServletRequest httpRequest) {

        Integer userId = extractUserId(httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "Notification marked as read",
                notificationService.markAsRead(userId, notificationId)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(HttpServletRequest httpRequest) {
        Integer userId = extractUserId(httpRequest);
        notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }

    private Integer extractUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);
        if (userId == null) {
            throw new IllegalStateException("User ID not found in request");
        }
        return (Integer) userId;
    }
}

