package com.logistics.service.common;

import com.logistics.dto.NotificationDto;
import com.logistics.entity.Office;
import com.logistics.entity.Notification;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.User;
import com.logistics.mapper.NotificationMapper;
import com.logistics.repository.NotificationRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.common.notification.NotificationSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.NotificationResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.NotificationSpecification;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
            Integer userId, Integer creatorId, String relatedType, String relatedId) {
        if (userId == null) {
            return;
        }

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

        NotificationDto dto = NotificationMapper.toDto(notification);

        messagingTemplate.convertAndSendToUser(
                user.getId().toString(),
                "/queue/notifications",
                dto);
    }

    @Transactional
    public ApiResponse<NotificationResponse> getNotifications(Integer userId, NotificationSearchRequest request) {
        try {
            int page = request.getPage();
            int limit = request.getLimit();
            String search = request.getSearch();
            Boolean isRead = request.getIsRead();
            
            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());

            Specification<Notification> spec = NotificationSpecification.unrestrictedNotification()
                    .and(NotificationSpecification.userId(userId))
                    .and(NotificationSpecification.isRead(isRead))
                    .and(NotificationSpecification.search(search))
                    .and(NotificationSpecification.fetchCreator());

            Page<Notification> pageData = notificationRepository.findAll(spec, pageable);

            List<NotificationDto> notifications = pageData.getContent()
                    .stream()
                    .map(NotificationMapper::toDto)
                    .toList();

            int total = (int) pageData.getTotalElements();
            int unreadCount = notificationRepository.countUnreadByUserId(userId);

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            NotificationResponse data = new NotificationResponse();
            data.setNotifications(notifications);
            data.setPagination(pagination);
            data.setUnreadCount(unreadCount);

            return new ApiResponse<>(true, "Lấy danh sách thông báo thành công", data);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách thông báo: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<NotificationResponse> markAsRead(Integer userId, Integer notificationId) {
        try {
            Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId);
            if (notification == null) {
                return new ApiResponse<>(false, "Không tìm thấy thông báo", null);
            }

            notification.setIsRead(true);
            notificationRepository.save(notification);

            return new ApiResponse<>(true, "Đã đánh dấu thông báo đã đọc", null);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi đánh dấu thông báo đã đọc: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<NotificationResponse> markAllAsRead(Integer userId) {
        try {
            notificationRepository.markAllAsReadByUserId(userId);
            return new ApiResponse<>(true, "Đã đánh dấu tất cả thông báo đã đọc", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi đánh dấu tất cả thông báo đã đọc: " + e.getMessage(), null);
        }
    }

    public void notifyOfficeManagerOnShippingRequestAssigned(Office office, ShippingRequest req) {
        if (office == null || req == null) return;
        try {
            com.logistics.entity.Employee managerEmp = office.getManager();
            if (managerEmp != null && managerEmp.getUser() != null) {
                User managerUser = managerEmp.getUser();
                String title = "Yêu cầu hỗ trợ mới được phân công";
                String message = "Bạn vừa được phân công xử lý yêu cầu hỗ trợ #" + req.getCode();
                this.create(title, message, "SHIPPING_REQUEST_ASSIGN", managerUser.getId(), null, "ShippingRequest", req.getId() + "");
            }
        } catch (Exception ignored) {
        }
    }
}