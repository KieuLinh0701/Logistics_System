package com.logistics.dto.notification;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private Integer id;
    private String title;
    private String message;
    private String type;
    private Boolean isRead;
    private String relatedId;
    private String relatedType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String creatorName;
}
