package com.logistics.controller.leave;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.leave.EmployeeLeaveDto;
import com.logistics.request.leave.ApproveLeaveRequest;
import com.logistics.request.leave.CreateLeaveRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.leave.EmployeeLeaveService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/leaves")
public class EmployeeLeaveController {

    private final EmployeeLeaveService leaveService;

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeLeaveDto>> createLeave(@Valid @RequestBody CreateLeaveRequest request) {
        return ResponseEntity.ok(leaveService.createLeave(request));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<EmployeeLeaveDto>>> getMyLeaves() {
        return ResponseEntity.ok(leaveService.getMyLeaves());
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelLeave(@PathVariable Integer id) {
        return ResponseEntity.ok(leaveService.cancelLeave(id));
    }

    @GetMapping("/office")
    public ResponseEntity<ApiResponse<List<EmployeeLeaveDto>>> getOfficeLeaves() {
        return ResponseEntity.ok(leaveService.getOfficeLeaves());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<EmployeeLeaveDto>> approveLeave(
            @PathVariable Integer id,
            @Valid @RequestBody ApproveLeaveRequest request) {
        return ResponseEntity.ok(leaveService.approveLeave(id, request));
    }
}