package com.logistics.entity;

import com.logistics.enums.EmployeeShift;
import com.logistics.enums.LeaveReasonType;
import com.logistics.enums.LeaveRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employee_leave_requests")
@EntityListeners(AuditingEntityListener.class)
public class EmployeeLeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @Column(nullable = false)
    private LocalDate leaveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeShift shift;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveReasonType reasonType;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String customReason;

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String employeeNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveRequestStatus status = LeaveRequestStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}