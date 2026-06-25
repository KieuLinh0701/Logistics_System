package com.logistics.entity;

import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.ShipmentType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shipments")
@EntityListeners(AuditingEntityListener.class)
@ToString(exclude = {"shipmentOrders"})
@EqualsAndHashCode(exclude = {"shipmentOrders"})
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

    // Liên kết tới Employee
    @ManyToOne
    @JoinColumn(name = "employee_id", referencedColumnName = "id", nullable = true)
    private Employee employee;

    // Liên kết 1-n với ShipmentOrder
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShipmentOrder> shipmentOrders = new ArrayList<>();

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

    @ManyToOne
    @JoinColumn(name = "from_office_id")
    private Office fromOffice;

    @ManyToOne
    @JoinColumn(name = "to_office_id")
    private Office toOffice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentType type;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private Employee createdBy; // Người tạo chuyến

    @PostPersist
    private void generateCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        this.code = "SM" + datePart + this.id;
    }

}
