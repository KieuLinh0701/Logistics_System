package com.logistics.service.common;

import com.logistics.entity.Office;
import com.logistics.entity.ShippingRequest;
import com.logistics.request.common.notification.NotificationSearchRequest;
import com.logistics.response.NotificationResponse;
import lombok.NonNull;

public interface NotificationService {
    void create(@NonNull String title, @NonNull String message, @NonNull String type,
                Integer userId, Integer creatorId, String relatedType, String relatedId);

    NotificationResponse getNotifications(Integer userId, NotificationSearchRequest request);

    void markAsRead(Integer userId, Integer notificationId);

    void markAllAsRead(Integer userId);

    void notifyOfficeManagerOnShippingRequestAssigned(Office office, ShippingRequest req);
}