// Khi tạo 1 promotion nếu không phải isGlobal có thể nhập và gán cho nhiều user hoặc 1 user nha
package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.PromotionDiscountType;
import com.logistics.enums.PromotionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promotions")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = false, unique = true)
    private String code;

    @Column(length = 255)
    private String title;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PromotionDiscountType discountType = PromotionDiscountType.PERCENTAGE;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private Boolean isGlobal = true; // true cho dùng chung, false cho riêng các user

    @Column(nullable = true)
    private Integer maxDiscountAmount;

    // Thời gian áp dụng promotion
    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    // Các điều kiện áp dụng
    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal minWeight;

    @Column(precision = 10, scale = 2)
    private BigDecimal maxWeight;

    @Column
    private Integer minOrdersCount; // Số đơn tối thiểu khách đã tạo

    @ManyToMany
    @JoinTable(name = "promotion_service_types", joinColumns = @JoinColumn(name = "promotion_id"), inverseJoinColumns = @JoinColumn(name = "service_type_id"))
    private List<ServiceType> serviceTypes = new ArrayList<>();

    @Column
    private Boolean firstTimeUser = false;  // Khách hàng mới

    @Column
    private Integer validMonthsAfterJoin; // Số tháng kể từ khi khách đăng ký null = không giới hạn

    @Column
    private Integer validYearsAfterJoin; // Số năm kể từ khi khách đăng ký null = không giới hạn

    // Tổng số lần sử dụng của promotion (toàn hệ thống)
    @Column(nullable = true)
    private Integer usageLimit;

    // Tổng số lần sử dụng tối đa của 1 user
    @Column(nullable = true)
    private Integer maxUsagePerUser;

    // **Giới hạn số lượt sử dụng theo ngày**
    @Column(nullable = true)
    private Integer dailyUsageLimitGlobal; // áp dụng cho global

    @Column(nullable = true)
    private Integer dailyUsageLimitPerUser; // áp dụng cho từng user

    @Column(nullable = false)
    private Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PromotionStatus status = PromotionStatus.ACTIVE;

    // Quan hệ 1-n với Order
    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Order> orders;

    // Quan hệ 1-n với UserPromotion
    @OneToMany(mappedBy = "promotion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPromotion> userPromotions;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}