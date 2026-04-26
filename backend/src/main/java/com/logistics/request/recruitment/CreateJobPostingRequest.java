package com.logistics.request.recruitment;

import com.logistics.enums.JobPostingStatus;
import com.logistics.enums.RecruitmentRoleType;
import com.logistics.enums.EmployeeShift;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobPostingRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không vượt quá 255 ký tự")
    private String title;

    @NotBlank(message = "Mô tả công việc không được để trống")
    private String description;

    @NotNull(message = "Vai trò tuyển dụng không được để trống")
    private RecruitmentRoleType roleType;

    @NotNull(message = "Office không được để trống")
    private Integer officeId;

    private JobPostingStatus status;

    @NotNull(message = "Số lượng cần tuyển không được để trống")
    @jakarta.validation.constraints.Min(value = 1, message = "Số lượng cần tuyển phải lớn hơn 0")
    private Integer quantityNeeded;

    @NotNull(message = "Ca làm việc không được để trống")
    private EmployeeShift shift;
}
