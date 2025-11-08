package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.ShippingRate.ShippingRateRegionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_rates")
@Data
@Getter
@Setter
@Audited
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ShippingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 1 ShippingRate thuộc về 1 ServiceType
    @ManyToOne
    @JoinColumn(name = "service_type_id", nullable = false)
    @NotAudited
    private ServiceType serviceType;

    // Loại khu vực
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ShippingRateRegionType regionType;

    // Trọng lượng áp dụng
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal weightFrom;

    @Column(precision = 10, scale = 2)
    private BigDecimal weightTo;

    // Giá cước
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    // Đơn vị tính (kg mặc định 0.5)
    @Column(precision = 10, scale = 2)
    private BigDecimal unit = new BigDecimal("0.5");

    // Giá thêm cho mỗi đơn vị vượt quá weightFrom nếu weightTo null
    @Column(precision = 10, scale = 2)
    private BigDecimal extraPrice;

    // Timestamps
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}