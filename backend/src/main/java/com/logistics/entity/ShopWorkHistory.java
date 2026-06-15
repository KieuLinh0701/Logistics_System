package com.logistics.entity;

import java.time.LocalDateTime;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "shop"})
@Table(name = "shop_work_histories")
@EntityListeners(AuditingEntityListener.class)
public class ShopWorkHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Nhân viên
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Cửa hàng/Chủ shop quản lý
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private User shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(nullable = false)
    private Boolean isCurrent = true; // true: đang làm, false: đã nghỉ/chuyển đi

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String note; // Ghi chú: ví dụ "Chuyển kho", "Nghỉ việc", v.v.

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}