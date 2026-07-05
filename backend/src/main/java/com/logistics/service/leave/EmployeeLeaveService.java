package com.logistics.service.leave;

import com.logistics.dto.leave.EmployeeLeaveDto;
import com.logistics.request.leave.ApproveLeaveRequest;
import com.logistics.request.leave.CreateLeaveRequest;

import java.util.List;

public interface EmployeeLeaveService {
    EmployeeLeaveDto createLeave(CreateLeaveRequest request);
    List<EmployeeLeaveDto> getMyLeaves();
    void cancelLeave(Integer leaveId);
    List<EmployeeLeaveDto> getOfficeLeaves();
    EmployeeLeaveDto approveLeave(Integer leaveId, ApproveLeaveRequest request);
}
