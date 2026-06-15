package com.logistics.controller.recruitment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.recruitment.JobApplicationDto;
import com.logistics.dto.recruitment.JobPostingDto;
import com.logistics.enums.JobApplicationStatus;
import com.logistics.enums.JobPostingStatus;
import com.logistics.request.recruitment.CreateJobApplicationRequest;
import com.logistics.request.recruitment.CreateJobPostingRequest;
import com.logistics.request.recruitment.UpdateJobApplicationStatusRequest;
import com.logistics.request.recruitment.UpdateJobPostingRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.recruitment.RecruitmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@Validated
@Tag(name = "Recruitment", description = "API quản lý tuyển dụng nhân sự logistics")
@RequestMapping
public class RecruitmentController {

    @Autowired
    private RecruitmentService recruitmentService;

    @PostMapping("/api/jobs")
    @Operation(summary = "Tạo tin tuyển dụng", description = "Admin hoặc Manager tạo mới tin tuyển dụng")
    public ResponseEntity<ApiResponse<JobPostingDto>> createJob(@Valid @RequestBody CreateJobPostingRequest request) {
        ApiResponse<JobPostingDto> response = recruitmentService.createJob(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/api/jobs")
    @Operation(summary = "Danh sách tin tuyển dụng", description = "Lấy danh sách tin tuyển dụng có phân trang/lọc")
    public ResponseEntity<ApiResponse<ListResponse<JobPostingDto>>> listJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) JobPostingStatus status,
            @RequestParam(required = false) Integer officeId) {
        return ResponseEntity.ok(recruitmentService.listJobs(page, limit, status, officeId));
    }

    @GetMapping("/api/jobs/{id}")
    @Operation(summary = "Chi tiết tin tuyển dụng")
    public ResponseEntity<ApiResponse<JobPostingDto>> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getJobById(id));
    }

    @PutMapping("/api/jobs/{id}")
    @Operation(summary = "Cập nhật tin tuyển dụng")
    public ResponseEntity<ApiResponse<JobPostingDto>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobPostingRequest request) {
        return ResponseEntity.ok(recruitmentService.updateJob(id, request));
    }

    @DeleteMapping("/api/jobs/{id}")
    @Operation(summary = "Xóa tin tuyển dụng")
    public ResponseEntity<ApiResponse<String>> deleteJob(@PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.deleteJob(id));
    }

    @PostMapping("/api/job-applications")
    @Operation(summary = "Ứng viên nộp hồ sơ", description = "Public user nộp hồ sơ ứng tuyển")
    public ResponseEntity<ApiResponse<JobApplicationDto>> createApplication(
            @Valid @RequestBody CreateJobApplicationRequest request) {
        ApiResponse<JobApplicationDto> response = recruitmentService.createApplication(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/api/job-applications")
    @Operation(summary = "Danh sách hồ sơ ứng tuyển", description = "Admin/Manager xem toàn bộ, Branch Manager xem theo bưu cục")
    public ResponseEntity<ApiResponse<ListResponse<JobApplicationDto>>> listApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long jobPostingId,
            @RequestParam(required = false) JobApplicationStatus status) {
        return ResponseEntity.ok(recruitmentService.listApplications(page, limit, jobPostingId, status));
    }

    @GetMapping("/api/job-applications/{id}")
    @Operation(summary = "Chi tiết hồ sơ ứng tuyển")
    public ResponseEntity<ApiResponse<JobApplicationDto>> getApplication(@PathVariable Long id) {
        return ResponseEntity.ok(recruitmentService.getApplicationById(id));
    }

    @PutMapping("/api/job-applications/{id}/status")
    @Operation(summary = "Duyệt trạng thái hồ sơ", description = "PENDING -> REVIEWING -> APPROVED/REJECTED")
    public ResponseEntity<ApiResponse<JobApplicationDto>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobApplicationStatusRequest request) {
        return ResponseEntity.ok(recruitmentService.updateApplicationStatus(id, request));
    }
}
