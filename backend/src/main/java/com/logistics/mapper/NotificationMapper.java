package com.logistics.mapper;

import com.logistics.dto.NotificationDto;
import com.logistics.entity.Notification;

public class NotificationMapper {

    public static NotificationDto toDto(Notification entity) {
        if (entity == null) {
            return null;
        }

        return new NotificationDto(
                entity.getId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getType(),
                entity.getIsRead(),
                entity.getRelatedId(),
                entity.getRelatedType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreator() != null ? entity.getCreator().getFullName() : null);
    }
}