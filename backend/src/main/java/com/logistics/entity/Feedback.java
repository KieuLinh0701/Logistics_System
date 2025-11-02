package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Audited
@Table(name = "feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Một feedback thuộc về một đơn hàng
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_feedback_order"))
    private Order order;

    // Một feedback được tạo bởi một user (có thể null nếu anonymous)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true, foreignKey = @ForeignKey(name = "fk_feedback_user"))
    private User user;

    @Column(nullable = false)
    private Integer rating; // Điểm đánh giá từ 1-5

    @Lob
    private String comment; // Nhận xét chi tiết

    @Column
    private Integer serviceRating; // Đánh giá chất lượng dịch vụ 1-5

    @Column
    private Integer deliveryRating; // Đánh giá thái độ nhân viên giao hàng 1-5

    @Column(nullable = false)
    private Boolean isAnonymous = false; // Đánh giá ẩn danh

    @Column(nullable = false)
    private Boolean isEdited = false; // Chỉ cho phép sửa 1 lần

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}