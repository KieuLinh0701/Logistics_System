package com.logistics.repository;

import com.logistics.dto.notification.NotificationDTO;
import com.logistics.entity.Notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("""
                SELECT new com.logistics.dto.notification.NotificationDTO(
                    n.id, n.title, n.message, n.type, n.isRead, n.relatedId, n.relatedType, n.createdAt, n.updatedAt, CONCAT(c.lastName, ' ', c.firstName)
                )
                FROM Notification n 
                LEFT JOIN n.creator c
                WHERE n.user.id = :userId
                  AND (:isRead IS NULL OR n.isRead = :isRead)
                  AND (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%'))
                       OR LOWER(n.message) LIKE LOWER(CONCAT('%', :search, '%')))
                ORDER BY n.createdAt DESC
            """)
    Page<NotificationDTO> findAllByUserIdAndFilters(
            @Param("userId") Integer userId,
            @Param("isRead") Boolean isRead,
            @Param("search") String search,
            Pageable pageable);

    // Đếm tổng thông báo của user
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId")
    int countByUserId(Integer userId);

    // Đếm số thông báo chưa đọc
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    int countUnreadByUserId(Integer userId);

    // Tìm thông báo theo id + userId
    Notification findByIdAndUserId(Integer id, Integer userId);

    // Đánh dấu tất cả đã đọc
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(Integer userId);
}