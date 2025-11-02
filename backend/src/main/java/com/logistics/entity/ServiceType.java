package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.ServiceType.ServiceTypeStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "service_types")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ServiceType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Tên dịch vụ
    @Column(length = 100, nullable = false, unique = true, columnDefinition = "VARCHAR(100)")
    private String name;

    // Thời gian giao hàng (VD: "1-2 ngày", "Trong ngày")
    @Column(columnDefinition = "TEXT")
    private String deliveryTime;

    // Trạng thái hoạt động
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private ServiceTypeStatus status = ServiceTypeStatus.ACTIVE;

    // Quan hệ 1-n với ShippingRate
    @OneToMany(mappedBy = "serviceType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShippingRate> rates;

    // Thời gian tạo/cập nhật
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}