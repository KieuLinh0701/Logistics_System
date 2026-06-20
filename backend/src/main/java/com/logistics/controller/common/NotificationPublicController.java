package com.logistics.controller.common;

import com.logistics.request.common.notification.NotificationSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.NotificationResponse;
import com.logistics.service.common.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification", description = "Quản lý thông báo người dùng")
public class NotificationPublicController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotifications(@Valid NotificationSearchRequest notificationSearchRequest,
    HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        NotificationResponse result = notificationService.getNotifications(userId, notificationSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Integer notificationId,
    HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}