package com.resumeai.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.notification.dto.request.CreateNotificationRequest;
import com.resumeai.notification.dto.response.NotificationResponse;
import com.resumeai.notification.security.JwtAuthenticationFilter;
import com.resumeai.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private static final Integer USER_ID = 1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    private NotificationResponse buildNotificationResponse(Integer id, boolean isRead) {
        return NotificationResponse.builder()
                .notificationId(id)
                .userId(USER_ID)
                .type("EXPORT")
                .title("Resume Exported")
                .message("Your resume was exported successfully")
                .referenceId(10)
                .referenceType("EXPORT")
                .isRead(isRead)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/notifications — should create notification")
    void createNotification_shouldReturn201() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setType("EXPORT");
        request.setTitle("Resume Exported");
        request.setMessage("Your resume was exported successfully");
        request.setReferenceId(10);
        request.setReferenceType("EXPORT");

        NotificationResponse response = buildNotificationResponse(1, false);
        when(notificationService.createNotification(eq(USER_ID), any(CreateNotificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.notificationId").value(1));

        verify(notificationService).createNotification(eq(USER_ID), any(CreateNotificationRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/notifications — should return user's notifications")
    void getMyNotifications_shouldReturnList() throws Exception {
        List<NotificationResponse> responses = List.of(
                buildNotificationResponse(1, false),
                buildNotificationResponse(2, true)
        );
        when(notificationService.getMyNotifications(USER_ID)).thenReturn(responses);

        mockMvc.perform(get("/api/v1/notifications")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(notificationService).getMyNotifications(USER_ID);
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count — should return unread count")
    void getUnreadCount_shouldReturnCount() throws Exception {
        when(notificationService.getUnreadCount(USER_ID)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(3));

        verify(notificationService).getUnreadCount(USER_ID);
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/{id}/read — should mark as read")
    void markAsRead_shouldReturnUpdatedNotification() throws Exception {
        NotificationResponse response = buildNotificationResponse(1, true);
        when(notificationService.markAsRead(USER_ID, 1)).thenReturn(response);

        mockMvc.perform(put("/api/v1/notifications/1/read")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isRead").value(true));

        verify(notificationService).markAsRead(USER_ID, 1);
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/read-all — should mark all as read")
    void markAllAsRead_shouldReturnSuccess() throws Exception {
        doNothing().when(notificationService).markAllAsRead(USER_ID);

        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .requestAttr(JwtAuthenticationFilter.USER_ID_ATTRIBUTE, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(notificationService).markAllAsRead(USER_ID);
    }
}
