package com.logistics.service;

import com.logistics.dto.notification.NotificationDTO;
import com.logistics.entity.Notification;
import com.logistics.entity.User;
import com.logistics.repository.NotificationRepository;
import com.logistics.repository.UserRepository;
import com.logistics.response.NotificationResponse;
import com.logistics.response.Pagination;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void create(@NonNull String title, @NonNull String message, @NonNull String type,
            @NonNull Integer userId, Integer creatorId, String relatedType, String relatedId) {
        Notification notification = new Notification();

        User user = new User();
        user.setId(userId);

        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        User creator = null;
        if (creatorId != null) {
            creator = userRepository.findById(creatorId).orElse(null); 
        }
        notification.setCreator(creator);

        notificationRepository.save(notification);

        NotificationDTO dto = new NotificationDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getIsRead(),
                notification.getRelatedId(),
                notification.getRelatedType(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                notification.getCreator() != null ? notification.getCreator().getFullName() : null);

        // gửi notification riêng cho user
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                dto);
    }

    @Transactional
    public NotificationResponse getNotifications(Integer userId, int page, int limit, String search, Boolean isRead) {
        try {
            Pageable pageable = PageRequest.of(page - 1, limit);

            String searchValue = (search != null && !search.isBlank()) ? search : null;

            Page<NotificationDTO> notificationPage = notificationRepository
                    .findAllByUserIdAndFilters(userId, isRead, searchValue, pageable);

            List<NotificationDTO> notifications = notificationPage.getContent();
            int total = (int) notificationPage.getTotalElements();
            int unreadCount = notificationRepository.countUnreadByUserId(userId);

            Pagination pagination = new Pagination(total, page, limit, notificationPage.getTotalPages());

            NotificationResponse.NotificationData data = new NotificationResponse.NotificationData();
            data.setNotifications(notifications);
            data.setPagination(pagination);
            data.setUnreadCount(unreadCount);

            return new NotificationResponse(true, data, null);

        } catch (Exception e) {
            return new NotificationResponse(false, null, "Lỗi khi lấy danh sách thông báo: " + e.getMessage());
        }
    }

    @Transactional
    public NotificationResponse markAsRead(Integer userId, Integer notificationId) {
        try {
            Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId);
            if (notification == null) {
                return new NotificationResponse(false, null, "Không tìm thấy thông báo");
            }

            notification.setIsRead(true);
            notificationRepository.save(notification);

            return new NotificationResponse(true, null, "Đã đánh dấu thông báo đã đọc");

        } catch (Exception e) {
            return new NotificationResponse(false, null, "Lỗi khi đánh dấu thông báo đã đọc: " + e.getMessage());
        }
    }

    @Transactional
    public NotificationResponse markAllAsRead(Integer userId) {
        try {
            notificationRepository.markAllAsReadByUserId(userId);
            return new NotificationResponse(true, null, "Đã đánh dấu tất cả thông báo đã đọc");
        } catch (Exception e) {
            return new NotificationResponse(false, null, "Lỗi khi đánh dấu tất cả thông báo đã đọc: " + e.getMessage());
        }
    }
}