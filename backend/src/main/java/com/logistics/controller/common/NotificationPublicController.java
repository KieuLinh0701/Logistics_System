package com.logistics.controller.common;

import com.logistics.request.common.notification.NotificationSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.NotificationResponse;
import com.logistics.service.common.NotificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationPublicController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotifications(@Valid NotificationSearchRequest notificationSearchRequest,
    HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<NotificationResponse> result = notificationService.getNotifications(userId, notificationSearchRequest);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Integer notificationId,
    HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<NotificationResponse> result = notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAllAsRead(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<NotificationResponse> result = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(result);
    }
}