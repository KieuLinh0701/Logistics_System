package com.logistics.controller.shipper;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.shipper.CollectCODRequest;
import com.logistics.request.shipper.SubmitCODRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.CODShipperService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shipper/cod")
@Tag(name = "Shipper - COD", description = "Quản lý thu hộ (COD), nộp tiền thu hộ và lịch sử đối soát của nhân viên giao hàng")
public class CODShipperController {

    @Autowired
    private CODShipperService codShipperService;

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> collectCOD(@RequestBody CollectCODRequest request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(codShipperService.collectCOD(request)));
    }

    @PostMapping("/submit")
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
}
