package com.logistics.dto.recruitment;

import java.time.LocalDateTime;

import com.logistics.enums.JobPostingStatus;
import com.logistics.enums.RecruitmentRoleType;
import com.logistics.enums.EmployeeShift;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingDto {
    private Long id;
    private String title;
    private String description;
    private RecruitmentRoleType roleType;
    private Integer officeId;
    private String officeName;
    private JobPostingStatus status;
    private Integer quantityNeeded;
    private EmployeeShift shift;
    private Integer createdByAccountId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
