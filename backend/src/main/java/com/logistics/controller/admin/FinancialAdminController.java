package com.logistics.controller.admin;

import com.logistics.request.admin.CreateBatchRequest;
import com.logistics.request.admin.CreatePaymentSubmissionRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.dto.admin.AdminPaymentSubmissionListDto;
import com.logistics.entity.PaymentSubmissionBatch;
import com.logistics.service.admin.FinancialAdminService;
import com.logistics.utils.SecurityUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/financial")
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
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(service.listSubmissions(status));
    }

    @PutMapping("/submissions/{id}")
    public ResponseEntity<ApiResponse<Boolean>> processSubmission(
            @PathVariable Integer id,
            @RequestBody CreatePaymentSubmissionRequest form) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        Integer adminId = SecurityUtils.getAuthenticatedUserId();
        ApiResponse<Boolean> resp = service.processSubmission(adminId, id, form);
        if (!resp.isSuccess()) return ResponseEntity.status(400).body(resp);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<PaymentSubmissionBatch>> createBatch(@RequestBody CreateBatchRequest req) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        Integer adminId = SecurityUtils.getAuthenticatedUserId();
        ApiResponse<PaymentSubmissionBatch> resp = service.createBatch(adminId, req);
        if (!resp.isSuccess()) return ResponseEntity.status(400).body(resp);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/batches/{id}/complete")
    public ResponseEntity<ApiResponse<Boolean>> completeBatch(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        Integer adminId = SecurityUtils.getAuthenticatedUserId();
        ApiResponse<Boolean> resp = service.completeBatch(adminId, id);
        if (!resp.isSuccess()) return ResponseEntity.status(400).body(resp);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<ListResponse<PaymentSubmissionBatch>>> listBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer shipperId) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(service.listBatches(page, limit, search, status, shipperId));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Map<Integer, List<AdminPaymentSubmissionListDto>>>> pendingByShipper() {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(service.listPendingGroupedByShipper());
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<PaymentSubmissionBatch>> getBatch(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(service.getBatchById(id));
    }

    @GetMapping("/batches/export")
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
