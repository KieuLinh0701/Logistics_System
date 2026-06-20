package com.logistics.controller.leave;

import com.logistics.dto.leave.EmployeeLeaveDto;
import com.logistics.request.leave.ApproveLeaveRequest;
import com.logistics.request.leave.CreateLeaveRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.leave.EmployeeLeaveService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "Employee Leave", description = "Quản lý đơn xin nghỉ phép của nhân viên (Shipper, Driver) và xét duyệt (Manager)")
public class EmployeeLeaveController {

    private final EmployeeLeaveService leaveService;

    // Shipper, Driver
    @PostMapping("/api/leaves")
    public ResponseEntity<ApiResponse<EmployeeLeaveDto>> createLeave(@Valid @RequestBody CreateLeaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.createLeave(request)));
    }

    @GetMapping("/api/leaves/my")
    public ResponseEntity<ApiResponse<List<EmployeeLeaveDto>>> getMyLeaves() {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getMyLeaves()));
    }

    @PutMapping("/api/leaves/{id}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelLeave(@PathVariable Integer id) {
        leaveService.cancelLeave(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Manager
    @GetMapping("/api/manager/leaves/office")
    public ResponseEntity<ApiResponse<List<EmployeeLeaveDto>>> getOfficeLeaves() {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getOfficeLeaves()));
    }

    @PutMapping("/api/manager/leaves/{id}/approve")
    public ResponseEntity<ApiResponse<EmployeeLeaveDto>> approveLeave(
            @PathVariable Integer id,
            @Valid @RequestBody ApproveLeaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.approveLeave(id, request)));
    }
}