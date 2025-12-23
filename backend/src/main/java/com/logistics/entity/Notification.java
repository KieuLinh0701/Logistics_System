package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Người nhận thông báo
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    // Tiêu đề thông báo
    @Column(columnDefinition = "NVARCHAR(255)", nullable = false)
    private String title;

    // Nội dung thông báo
    @Column(name = "message", columnDefinition = "VARCHAR(500)")
    private String message;

    // Loại thông báo
    @Column(nullable = false)
    private String type;

    // Trạng thái đã đọc
    @Column(nullable = false)
    private Boolean isRead = false;

    // Id liên quan (có thể là order, feedback,...)
    @Column(name = "related_id")
    private String relatedId; // Chuyển qua string cho trackingNumber

    // Loại đối tượng liên quan
    @Column(name = "related_type")
    private String relatedType;

    // Thời gian tạo/cập nhật
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}