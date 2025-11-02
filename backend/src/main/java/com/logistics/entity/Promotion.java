package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.Promotion.PromotionDiscountType;
import com.logistics.enums.Promotion.PromotionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PromotionDiscountType discountType = PromotionDiscountType.PERCENTAGE;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @Column(nullable = true)
    private Integer minOrderValue = 0;

    @Column(nullable = true)
    private Integer maxDiscountAmount;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    // Tổng số lần sử dụng của promotion (toàn hệ thống)
    @Column(nullable = true)
    private Integer usageLimit;

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