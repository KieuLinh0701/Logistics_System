package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.FeeConfiguration.CodFeeType;
import com.logistics.enums.FeeConfiguration.FeeType;

@Entity
@Table(name = "fee_configurations")
@Data
@Getter
@Setter
@Audited
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class FeeConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Áp dụng theo loại dịch vụ (ShippingService)
    @ManyToOne
    @JoinColumn(name = "service_type_id")
    private ServiceType serviceType;

    // Loại phí
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private FeeType feeType;

    // Loại tính phí: %
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private CodFeeType calculationType;

    // Giá trị: 5 = 5% nếu calculationType = PERCENTAGE
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal feeValue;

    // Giá trị áp dụng tối thiểu của đơn hàng
    @Column(precision = 12, scale = 2)
    private BigDecimal minOrderFee;

    // Giá trị áp dụng tối đa của đơn hàng
    @Column(precision = 12, scale = 2)
    private BigDecimal maxOrderFee;

    // Trạng thái áp dụng
    @Column(nullable = false)
    private Boolean active = true;

    @Lob
    private String notes;

    // Thời gian tạo và cập nhật
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}