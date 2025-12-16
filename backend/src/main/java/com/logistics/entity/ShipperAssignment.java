package com.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Setter
@Getter
@Table(name = "shipper_assignments")
@Data
@Audited
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ShipperAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Shipper được phân công
    @ManyToOne
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    // Khu vực được phân công
    @Column(nullable = false)
    private Integer wardCode;

    @Column(nullable = false)
    private Integer cityCode;

    // Ngày bắt đầu hiệu lực
    @Column(nullable = false)
    private LocalDateTime startAt;

    // Ngày kết thúc phân công
    @Column(nullable = true)
    private LocalDateTime endAt;

    // Ghi chú thêm
    @Column(columnDefinition = "NVARCHAR(255)", length = 255)
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}