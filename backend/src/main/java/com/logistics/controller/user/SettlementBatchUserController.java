package com.logistics.controller.user;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.user.settlement.UserSettlementBatchListDto;
import com.logistics.dto.user.settlement.UserSettlementOrderDto;
import com.logistics.dto.user.settlement.UserSettlementSummaryResponse;
import com.logistics.dto.user.settlement.UserSettlementTransactionDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.SearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.SettlementBatchUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/user/settlement-batchs")
@Tag(name = "User - Settlement Batch", description = "Quản lý các đợt đối soát tài chính: xem tổng quan, chi tiết từng phiên, danh sách đơn hàng/giao dịch liên quan và xuất báo cáo đối soát")
public class SettlementBatchUserController {

    private final SettlementBatchUserService service;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<UserSettlementSummaryResponse>> getSummary(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.getSummary(userId)));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<UserSettlementBatchListDto>>> list(
            @Valid SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<UserSettlementBatchListDto> result = service.list(userId, searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserSettlementBatchListDto>> getBySettlementBatchId(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        UserSettlementBatchListDto result = service.getBySettlementBatchId(
                userId,
                id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<ListResponse<UserSettlementOrderDto>>> getOrdersBySettlementBatchId(
            @PathVariable Integer id,
            @Valid SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<UserSettlementOrderDto> result = service.getOrdersBySettlementBatchId(
                userId,
                id,
                searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<UserSettlementTransactionDto>>> getSettlementTransactionsBySettlementBatchId(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        List<UserSettlementTransactionDto> result = service
                .getSettlementTransactionsBySettlementBatchId(
                        userId,
                        id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/export")
    @Audit(
            entity = EntityType.SETTLEMENT_BATCH,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.SETTLEMENT_EXPORT_LIST
    )
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            SearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, searchRequest);

        String fileName = "UTE Logistics_Báo cáo lịch sử đối soát.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    @GetMapping("/export/{id}")
    @Audit(
            entity = EntityType.SETTLEMENT_BATCH,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.SETTLEMENT_EXPORT_DETAIL,
            params = {"id"}
    )
    public ResponseEntity<byte[]> exportById(
            HttpServletRequest request,
            @PathVariable Integer id) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.exportById(userId, id);

        String fileName = "UTE Logistics_Báo cáo chi tiêt phiên đối soát.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}