package com.logistics.dto.leave;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLeaveDto {
    private Integer id;
    private Integer employeeId;
    private String employeeName;
    private Integer officeId;
    private LocalDate leaveDate;
    private String shift;
    private String reasonType;
    private String customReason;
    private String employeeNote;
    private String reasonDisplay;
    private String status;
    private Integer approvedById;
    private String approvedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}