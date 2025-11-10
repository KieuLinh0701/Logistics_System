package com.logistics.response;

import java.util.List;

import com.logistics.dto.notification.NotificationDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private boolean success;
    private NotificationData data;
    private String message;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationData {
        private List<NotificationDTO> notifications;
        private Pagination pagination;
        private int unreadCount;
    }
}