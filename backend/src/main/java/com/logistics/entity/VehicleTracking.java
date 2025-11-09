// Bảng mới theo dõi vị trí xe và cho hiển thị vị trí trên bản đồ và đường đã đi được
package com.logistics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "vehicle_trackings")
@EntityListeners(AuditingEntityListener.class)
public class VehicleTracking {
    @Id 
    @GeneratedValue
    private Integer id;

    // Liên kết tới xe
    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    // Liên kết tới chuyến hàng
    @ManyToOne
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(precision = 10, scale = 6, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 6, nullable = false)
    private BigDecimal longitude;

    // Tốc độ di chuyển
    @Column(precision = 6, scale = 2, nullable = false)
    private BigDecimal speed;

    // Thời gian tạo
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;
}