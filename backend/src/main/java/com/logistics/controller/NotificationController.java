package com.logistics.controller;

import com.logistics.response.NotificationResponse;
import com.logistics.security.UserPrincipal;
import com.logistics.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationResponse> getNotifications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isRead
    ) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = principal.getUser().getId();

        NotificationResponse result = notificationService.getNotifications(userId, page, limit, search, isRead);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Integer notificationId
    ) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = principal.getUser().getId();

        NotificationResponse result = notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<NotificationResponse> markAllAsRead() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = principal.getUser().getId();

        NotificationResponse result = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(result);
    }

    // Tạo thông báo mới
    // @PostMapping
    // public ResponseEntity<NotificationResponse> createNotification(
    //         @RequestBody No dto
    // ) {
    //     NotificationResponse result = notificationService.createNotification(dto);
    //     return ResponseEntity.status(result.isSuccess() ? 201 : 400).body(result);
    // }
}