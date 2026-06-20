// Bảng mới theo dõi vị trí xe và cho hiển thị vị trí trên bản đồ và đường đã đi được
package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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