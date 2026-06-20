package com.logistics.service.common;

import com.logistics.dto.NotificationDto;
import com.logistics.entity.Notification;
import com.logistics.entity.Office;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.User;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.NotificationErrorCode;
import com.logistics.mapper.NotificationMapper;
import com.logistics.repository.NotificationRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.common.notification.NotificationSearchRequest;
import com.logistics.response.NotificationResponse;
import com.logistics.response.Pagination;
import com.logistics.specification.NotificationSpecification;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
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
    public NotificationResponse getNotifications(Integer userId, NotificationSearchRequest request) {
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

            return data;
    }

    @Transactional
    public void markAsRead(Integer userId, Integer notificationId) {
            Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId);
            if (notification == null) {
                throw  new AppException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
            }

            notification.setIsRead(true);
            notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
            notificationRepository.markAllAsReadByUserId(userId);
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