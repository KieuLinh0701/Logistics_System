package com.logistics.controller.admin;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.admin.AdminPaymentSubmissionListDto;
import com.logistics.entity.PaymentSubmissionBatch;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.admin.CreatePaymentSubmissionRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.admin.FinancialAdminService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/financial")
@Tag(name = "Admin - Financial", description = "Quản lý đối soát và thanh toán")
public class FinancialAdminController {

    @Autowired
    private FinancialAdminService service;

    private boolean isNotAdmin() {
        return !SecurityUtils.hasRole("admin");
    }

    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<ListResponse<AdminPaymentSubmissionListDto>>> listSubmissions(
            @RequestParam(required = false) String status) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(service.listSubmissions(status)));
    }

    @PutMapping("/submissions/{id}")
    @Audit(
            entity = EntityType.PAYMENT_SUBMISSION,
            action = AuditLogAction.PROCESS,
            description = AuditLogDescriptionConstant.PAYMENT_SUBMISSION_PROCESSING,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> processSubmission(
            @PathVariable Integer id,
            @RequestBody CreatePaymentSubmissionRequest form) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        Integer adminId = SecurityUtils.getAuthenticatedUserId();
        service.processSubmission(adminId, id, form);
        return ResponseEntity.ok(ApiResponse.success("Xử lý đối soát thành công"));
    }

    @PostMapping("/batches/{id}/complete")
    @Audit(
            entity = EntityType.PAYMENT_SUBMISSION,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.PAYMENT_SUBMISSION_BATCH_COMPLETE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> completeBatch(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        Integer adminId = SecurityUtils.getAuthenticatedUserId();
        service.completeBatch(adminId, id);
        return ResponseEntity.ok(ApiResponse.success("Hoàn thành đợt đối soát thành công"));
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<ListResponse<PaymentSubmissionBatch>>> listBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer shipperId) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(service.listBatches(page, limit, search, status, shipperId)));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Map<Integer, List<AdminPaymentSubmissionListDto>>>> pendingByShipper() {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(service.listPendingGroupedByShipper()));
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<PaymentSubmissionBatch>> getBatch(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(service.getBatchById(id)));
    }

    @GetMapping("/batches/export")
    @Audit(
            entity = EntityType.PAYMENT_SUBMISSION_BATCH,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.PAYMENT_SUBMISSION_BATCH_EXPORT
    )
    public ResponseEntity<byte[]> exportBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer shipperId) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).build();
        }
        byte[] data = service.exportBatches(page, limit, search, status, shipperId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String fileName = "batches_export.xlsx";
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        return ResponseEntity.ok().headers(headers).body(data);
    }

    @GetMapping("/submissions/export")
    @Audit(
            entity = EntityType.PAYMENT_SUBMISSION,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.PAYMENT_SUBMISSION_EXPORT
    )
    public ResponseEntity<byte[]> exportSubmissions(@RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String search) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).build();
        }
        byte[] data = service.exportSubmissions(status, search);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String fileName = "submissions_export.xlsx";
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
