package com.logistics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.ShipmentStatus;

@Entity
@Audited
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipments")
@EntityListeners(AuditingEntityListener.class)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = true, unique = true)
    private String code; // Thêm này cho mã chuyến (SM_NGÀY THÁNG NĂM TẠO_id)

    // Liên kết tới Vehicle
    @ManyToOne
    @JoinColumn(name = "vehicle_id", referencedColumnName = "id", nullable = true)
    private Vehicle vehicle;

    // Liên kết tới User
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
    private User user;

    // Liên kết 1-n với ShipmentOrder
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<ShipmentOrder> shipmentOrders;

    // Trạng thái chuyến hàng
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    // Thời gian tạo và cập nhật
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void validateTimes() {
        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    // Bưu cục bắt đầu
    @ManyToOne
    @JoinColumn(name = "from_office_id", nullable = false)
    private Office fromOffice;

    // Bưu cục đích
    @ManyToOne
    @JoinColumn(name = "to_office_id", nullable = false)
    private Office toOffice;
}
