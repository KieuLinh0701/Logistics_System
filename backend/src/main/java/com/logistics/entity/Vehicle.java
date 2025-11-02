package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.Vehicle.VehicleStatus;
import com.logistics.enums.Vehicle.VehicleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Audited
@Table(name = "vehicles")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleType type = VehicleType.TRUCK;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String description;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    // Thời gian tạo và cập nhật
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Thêm các trường mới

    // Lần bảo trì gần nhất
    private LocalDateTime lastMaintenanceAt;

    // Lịch bảo trì kế tiếp
    private LocalDateTime nextMaintenanceDue;

    // Thêm vị trí hiện tại theo GPS
    @Column(precision = 10, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6)
    private BigDecimal longitude;

    @Column(length = 64)
    private String gpsDeviceId;
}