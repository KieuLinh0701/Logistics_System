package com.logistics.response;

import com.logistics.dto.NotificationDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private List<NotificationDto> notifications;
    private Pagination pagination;
    private int unreadCount;
}