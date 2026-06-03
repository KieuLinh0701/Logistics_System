package com.logistics.entity;

import com.logistics.enums.ShipperVehicleStatus;
import com.logistics.enums.ShipperVehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "shipper_vehicles")
@EntityListeners(AuditingEntityListener.class)
public class ShipperVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", referencedColumnName = "id", nullable = false, unique = true)
    private Employee shipper;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private ShipperVehicleType vehicleType;

    @Column(name = "max_orders", nullable = false)
    private Integer maxOrders;

    @Column(name = "max_weight_kg", nullable = false)
    private Integer maxWeightKg;

    @Column(name = "current_orders", nullable = false)
    private Integer currentOrders = 0;

    @Column(name = "current_weight_kg", precision = 10, scale = 2, nullable = false)
    private BigDecimal currentWeightKg = BigDecimal.ZERO;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShipperVehicleStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
