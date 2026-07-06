package com.logistics.controller.shipper;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.shipper.CollectCODRequest;
import com.logistics.request.shipper.SubmitCODRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.CODShipperService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shipper/cod")
@RequiredArgsConstructor
@Tag(name = "Shipper - COD", description = "Quản lý thu hộ (COD), nộp tiền thu hộ và lịch sử đối soát của nhân viên giao hàng")
public class CODShipperController {

    private final CODShipperService codShipperService;

    private boolean isNotShipper() {
        return !SecurityUtils.hasRole("shipper");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCODTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(codShipperService.getCODTransactions(page, limit, status, dateFrom, dateTo)));
    }

    @PostMapping("/collect")
    @Audit(
            entity = EntityType.PAYMENT_SUBMISSION,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.COD_COLLECT
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> collectCOD(@RequestBody CollectCODRequest request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(codShipperService.collectCOD(request)));
    }

    @PostMapping("/submit")
    @Audit(
            entity = EntityType.PAYMENT_SUBMISSION,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.COD_SUBMIT
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitCOD(@RequestBody SubmitCODRequest request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(codShipperService.submitCOD(request)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCODSubmissionHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(codShipperService.getCODSubmissionHistory(page, limit, status, dateFrom, dateTo)));
    }

    @GetMapping("/batch-history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCODBatchHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(codShipperService.getCODBatchHistory(page, limit, status, dateFrom, dateTo)));
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCODBatchDetail(@PathVariable Long batchId) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(codShipperService.getCODBatchDetail(batchId)));
    }
}
