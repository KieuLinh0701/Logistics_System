package com.logistics.request.recruitment;

import com.logistics.enums.EmployeeShift;
import com.logistics.enums.JobPostingStatus;
import com.logistics.enums.RecruitmentRoleType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobPostingRequest {

    @Size(max = 255, message = "Tiêu đề không vượt quá 255 ký tự")
    private String title;

    private String description;

    private RecruitmentRoleType roleType;

    private Integer officeId;

    private JobPostingStatus status;

    @jakarta.validation.constraints.Min(value = 1, message = "Số lượng cần tuyển phải lớn hơn 0")
    private Integer quantityNeeded;

    private EmployeeShift shift;
}
