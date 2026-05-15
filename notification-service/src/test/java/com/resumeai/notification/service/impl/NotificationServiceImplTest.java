package com.resumeai.notification.service.impl;

import com.resumeai.notification.dto.request.CreateNotificationRequest;
import com.resumeai.notification.dto.response.NotificationResponse;
import com.resumeai.notification.entity.Notification;
import com.resumeai.notification.exception.ResourceNotFoundException;
import com.resumeai.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final Integer USER_ID = 1;
    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .notificationId(1)
                .userId(USER_ID)
                .type("EXPORT")
                .title("Resume Exported")
                .message("Your resume was exported successfully")
                .referenceId(10)
                .referenceType("EXPORT")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createNotification — should save and return notification")
    void createNotification_shouldSaveAndReturn() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setType("EXPORT");
        request.setTitle("Resume Exported");
        request.setMessage("Your resume was exported successfully");
        request.setReferenceId(10);
        request.setReferenceType("EXPORT");

        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        NotificationResponse result = notificationService.createNotification(USER_ID, request);

        assertThat(result).isNotNull();
        assertThat(result.getNotificationId()).isEqualTo(1);
        assertThat(result.getType()).isEqualTo("EXPORT");
        assertThat(result.getIsRead()).isFalse();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("getMyNotifications — should return user's notifications")
    void getMyNotifications_shouldReturnList() {
        Notification second = Notification.builder()
                .notificationId(2)
                .userId(USER_ID)
                .type("ATS")
                .title("ATS Score Updated")
                .message("Your ATS score improved")
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(sampleNotification, second));

        List<NotificationResponse> result = notificationService.getMyNotifications(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNotificationId()).isEqualTo(1);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(USER_ID);
    }

    @Test
    @DisplayName("getUnreadCount — should return count of unread notifications")
    void getUnreadCount_shouldReturnCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(USER_ID)).thenReturn(5L);

        long count = notificationService.getUnreadCount(USER_ID);

        assertThat(count).isEqualTo(5L);
        verify(notificationRepository).countByUserIdAndIsReadFalse(USER_ID);
    }

    @Test
    @DisplayName("markAsRead — should mark notification as read")
    void markAsRead_shouldMarkAsRead() {
        when(notificationRepository.findById(1)).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse result = notificationService.markAsRead(USER_ID, 1);

        assertThat(result.getIsRead()).isTrue();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("markAsRead — should throw when notification not found")
    void markAsRead_shouldThrowWhenNotFound() {
        when(notificationRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, 999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("markAsRead — should throw when user doesn't own notification")
    void markAsRead_shouldThrowWhenNotOwned() {
        Notification otherUserNotification = Notification.builder()
                .notificationId(1)
                .userId(999)
                .type("EXPORT")
                .title("Test")
                .message("Test")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(1)).thenReturn(Optional.of(otherUserNotification));

        assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("markAllAsRead — should update all notifications to read")
    void markAllAsRead_shouldUpdateAll() {
        Notification unread1 = Notification.builder()
                .notificationId(1).userId(USER_ID).type("A").title("T").message("M").isRead(false).createdAt(LocalDateTime.now()).build();
        Notification unread2 = Notification.builder()
                .notificationId(2).userId(USER_ID).type("B").title("T2").message("M2").isRead(false).createdAt(LocalDateTime.now()).build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(unread1, unread2));

        notificationService.markAllAsRead(USER_ID);

        assertThat(unread1.getIsRead()).isTrue();
        assertThat(unread2.getIsRead()).isTrue();
        verify(notificationRepository).saveAll(anyList());
    }
}
