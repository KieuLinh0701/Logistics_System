package com.logistics.controller.recruitment;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.recruitment.JobApplicationDto;
import com.logistics.dto.recruitment.JobPostingDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@Tag(name = "Recruitment", description = "API quản lý tuyển dụng nhân sự logistics")
@RequestMapping
public class RecruitmentController {

    @Autowired
    private RecruitmentService recruitmentService;

    @PostMapping("/api/jobs")
    @Audit(
            entity = EntityType.JOB_POSTING,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.JOB_POSTING_CREATE
    )
    @Operation(summary = "Tạo tin tuyển dụng", description = "Admin hoặc Manager tạo mới tin tuyển dụng")
    public ResponseEntity<ApiResponse<JobPostingDto>> createJob(@Valid @RequestBody CreateJobPostingRequest request) {
        JobPostingDto response = recruitmentService.createJob(request);
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @GetMapping("/api/jobs")
    @Operation(summary = "Danh sách tin tuyển dụng", description = "Lấy danh sách tin tuyển dụng có phân trang/lọc")
    public ResponseEntity<ApiResponse<ListResponse<JobPostingDto>>> listJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) JobPostingStatus status,
            @RequestParam(required = false) Integer officeId) {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.listJobs(page, limit, status, officeId)));
    }

    @GetMapping("/api/jobs/{id}")
    @Operation(summary = "Chi tiết tin tuyển dụng")
    public ResponseEntity<ApiResponse<JobPostingDto>> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.getJobById(id)));
    }

    @PutMapping("/api/jobs/{id}")
    @Audit(
            entity = EntityType.JOB_APPLICATION,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.JOB_POSTING_UPDATE,
            params = {"id"}
    )
    @Operation(summary = "Cập nhật tin tuyển dụng")
    public ResponseEntity<ApiResponse<JobPostingDto>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobPostingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.updateJob(id, request)));
    }

    @DeleteMapping("/api/jobs/{id}")
    @Audit(
            entity = EntityType.JOB_APPLICATION,
            action = AuditLogAction.DELETE,
            description = AuditLogDescriptionConstant.JOB_POSTING_DELETE,
            params = {"id"}
    )
    @Operation(summary = "Xóa tin tuyển dụng")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable Long id) {

        recruitmentService.deleteJob(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/api/job-applications")
    @Audit(
            entity = EntityType.JOB_APPLICATION,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.JOB_APPLICATION_CREATE
    )
    @Operation(summary = "Ứng viên nộp hồ sơ", description = "Public user nộp hồ sơ ứng tuyển")
    public ResponseEntity<ApiResponse<JobApplicationDto>> createApplication(
            @Valid @RequestBody CreateJobApplicationRequest request) {
        JobApplicationDto response = recruitmentService.createApplication(request);
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @GetMapping("/api/job-applications")
    @Operation(summary = "Danh sách hồ sơ ứng tuyển", description = "Admin/Manager xem toàn bộ, Branch Manager xem theo bưu cục")
    public ResponseEntity<ApiResponse<ListResponse<JobApplicationDto>>> listApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long jobPostingId,
            @RequestParam(required = false) JobApplicationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.listApplications(page, limit, jobPostingId, status)));
    }

    @GetMapping("/api/job-applications/{id}")
    @Operation(summary = "Chi tiết hồ sơ ứng tuyển")
    public ResponseEntity<ApiResponse<JobApplicationDto>> getApplication(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.getApplicationById(id)));
    }

    @PutMapping("/api/job-applications/{id}/status")
    @Audit(
            entity = EntityType.JOB_APPLICATION,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.JOB_APPLICATION_UPDATE_STATUS,
            params = {"id"}
    )
    @Operation(summary = "Duyệt trạng thái hồ sơ", description = "PENDING -> REVIEWING -> APPROVED/REJECTED")
    public ResponseEntity<ApiResponse<JobApplicationDto>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobApplicationStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recruitmentService.updateApplicationStatus(id, request)));
    }
}
