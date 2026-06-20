package com.logistics.controller.manager;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.shipment.ManagerOrdersShipmentSearchRequest;
import com.logistics.request.manager.shipment.ManagerShipmentAddEditRequest;
import com.logistics.request.manager.shipment.ManagerShipmentSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.manager.GetOrdersByShipmentIdManagerResponse;
import com.logistics.service.manager.ShipmentManagerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/manager/shipments")
@Tag(name = "Manager - Shipment", description = "Quản lý các chuyến hàng, tạo/cập nhật lộ trình và xuất báo cáo vận hành tại bưu cục")
public class ShipmentManagerController {

    private final ShipmentManagerService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<ManagerShipmentListDto>>> list(
            @Valid ManagerShipmentSearchRequest managerShipmentSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, managerShipmentSearchRequest)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetOrdersByShipmentIdManagerResponse>> getOrdersByShipmentId(
            @PathVariable Integer id,
            @Valid ManagerOrdersShipmentSearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        GetOrdersByShipmentIdManagerResponse result = service.getOrdersByShipmentId(userId, id,
                searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/employee-performance/{id}/export")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.SHIPMENT_EXPORT_PERFORMANCE,
            params = {"id"}
    )
    public ResponseEntity<byte[]> exportShipmentPerformance(HttpServletRequest request,
            @PathVariable Integer id,
            SearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.exportShipmentPerformance(userId, id, searchRequest);

        String fileName = "UTE Logistics_Báo cáo danh sách chuyến hàng của nhân viên.xlsx";
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

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<ListResponse<ManagerShipmentListDto>>> getPendingShipments(
            SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerShipmentListDto> result = service.getPendingShipments(userId, searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{id}/cancel")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.CANCEL,
            description = AuditLogDescriptionConstant.SHIPMENT_CANCEL,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> cancelShipment(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.cancelShipment(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.SHIPMENT_CREATE
    )
    public ResponseEntity<ApiResponse<Void>> create(
            @RequestBody ManagerShipmentAddEditRequest editRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.create(userId, editRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.SHIPMENT_UPDATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Integer id,
            @RequestBody ManagerShipmentAddEditRequest editRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.update(userId, id, editRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.SHIPMENT_EXPORT_LIST
    )
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            ManagerShipmentSearchRequest managerShipmentSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, managerShipmentSearchRequest);

        String fileName = "UTE Logistics_Báo cáo danh sách chuyến hàng của bưu cục.xlsx";
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

    @GetMapping("/{id}/export")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.SHIPMENT_EXPORT_ORDERS,
            params = {"id"}
    )
    public ResponseEntity<byte[]> exportOrdersByShipmentId(HttpServletRequest request,
                                                            @PathVariable Integer id,
                                                            ManagerOrdersShipmentSearchRequest managerOrdersShipmentSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.exportOrdersByShipmentId(userId, id, managerOrdersShipmentSearchRequest);

        String fileName = "UTE Logistics_Báo cáo danh sách đơn hàng của chuyến hàng.xlsx";
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