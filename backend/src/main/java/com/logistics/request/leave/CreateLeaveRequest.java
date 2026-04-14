package com.logistics.request.leave;

import java.time.LocalDate;

import com.logistics.enums.EmployeeShift;
import com.logistics.enums.LeaveReasonType;

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
public class CreateLeaveRequest {
    @NotNull(message = "Ngày nghỉ không được để trống")
    private LocalDate leaveDate;

    @NotNull(message = "Ca nghỉ không được để trống")
    private EmployeeShift shift;

    @NotNull(message = "Lý do nghỉ không được để trống")
    private LeaveReasonType reasonType;

    @Size(max = 500, message = "Lý do riêng không được vượt quá 500 ký tự")
    private String customReason;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String employeeNote;
}