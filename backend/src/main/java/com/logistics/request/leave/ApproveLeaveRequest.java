package com.logistics.request.leave;

import com.logistics.enums.LeaveRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApproveLeaveRequest {
    @NotNull(message = "Trạng thái duyệt không được để trống")
    private LeaveRequestStatus status;
}