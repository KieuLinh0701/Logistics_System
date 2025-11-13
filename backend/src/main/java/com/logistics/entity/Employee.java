package com.logistics.entity;

import java.time.LocalDateTime;

import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.logistics.enums.EmployeeShift;
import com.logistics.enums.EmployeeStatus;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Audited
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employees")
@EntityListeners(AuditingEntityListener.class)
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = true, unique = true)
    private String code; // Thêm này cho mã nhân viên (EP_ID bưu cục_ID nhân viên)

    // Mỗi nhân viên liên kết với 1 user
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
    private User user;

    // Mỗi nhân viên thuộc 1 office
    @ManyToOne
    @JoinColumn(name = "office_id", referencedColumnName = "id", nullable = false)
    private Office office;

    @Column(nullable = false)
    private LocalDateTime hireDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeShift shift = EmployeeShift.FULL_DAY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}