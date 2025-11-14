package com.logistics.controller;

import com.logistics.request.notification.NotificationSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.NotificationResponse;
import com.logistics.service.NotificationService;
import com.logistics.utils.SecurityUtils;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotifications(@Valid NotificationSearchRequest request) {
        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            ApiResponse<NotificationResponse> response = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(401).body(response);
        }

        ApiResponse<NotificationResponse> result = notificationService.getNotifications(userId, request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Integer notificationId) {
        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            ApiResponse<NotificationResponse> response = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(401).body(response);
        }

        ApiResponse<NotificationResponse> result = notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAllAsRead() {
        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            ApiResponse<NotificationResponse> response = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(401).body(response);
        }

        ApiResponse<NotificationResponse> result = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(result);
    }

    // Tạo thông báo mới
    // @PostMapping
    // public ResponseEntity<NotificationResponse> createNotification(
    // @RequestBody No dto
    // ) {
    // NotificationResponse result = notificationService.createNotification(dto);
    // return ResponseEntity.status(result.isSuccess() ? 201 : 400).body(result);
    // }
}