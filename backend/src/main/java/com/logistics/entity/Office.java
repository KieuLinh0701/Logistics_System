package com.logistics.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.OfficeStatus;
import com.logistics.enums.OfficeType;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Audited
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "offices")
@EntityListeners(AuditingEntityListener.class)
public class Office {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Mã nội bộ, phải unique (PO_ID)

    // Thêm này cho in vận đơn
    @Column(length = 10, nullable = true)
    private String postalCode; // Mã bưu chính, không cần unique (PO_Mã tỉnh bưu cục_Mã xã bưu cục) dùng hiển thị trên vận đơn

    @Column(columnDefinition = "NVARCHAR(255)", nullable = false, unique = true)
    private String name; // Tên bưu cục, unique

    // Địa chỉ bưu cục
    @Column(nullable = false)
    private Integer cityCode;
    
    @Column(nullable = false)
    private Integer wardCode;

    @Column(columnDefinition = "NVARCHAR(255)", nullable = false)
    private String detail;

    @Column(nullable = false, precision = 12, scale = 7)
    private BigDecimal latitude; // Vĩ độ

    @Column(nullable = false, precision = 12, scale = 7)
    private BigDecimal longitude; // Kinh độ

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 15, unique = true)
    private String phoneNumber; // Số điện thoại liên hệ

    @Column(nullable = false)
    private LocalTime openingTime = LocalTime.of(7, 0); // Giờ mở cửa

    @Column(nullable = false)
    private LocalTime closingTime = LocalTime.of(17, 0); // Giờ đóng cửa

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfficeType type = OfficeType.POST_OFFICE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfficeStatus status = OfficeStatus.ACTIVE;

    // Quan hệ với nhân viên
    @OneToMany(mappedBy = "office", cascade = CascadeType.ALL)
    private List<Employee> employees;

    // Quản lý riêng
    @OneToOne
    @JoinColumn(name = "manager_id", nullable = true)
    private Employee manager;

    // Sức chứa tối đa (số kiện hàng tối đa của bưu cục)
    @Column
    private Integer capacity;

    // Ghi chú thêm
    @Lob
    private String notes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}