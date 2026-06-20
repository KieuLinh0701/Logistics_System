package com.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
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
