package com.logistics.controller.shipper;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.shipper.shipment.ShipperShipmentDetailDto;
import com.logistics.dto.shipper.shipment.ShipperShipmentListDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.shipper.ShipperOrdersShipmentSearchRequest;
import com.logistics.request.shipper.ShipperShipmentSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.shipper.ShipmentHistoryShipperService;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/shipper/shipments/history")
@Tag(name = "Shipper - Shipment", description = "Xem lịch sử chuyến hàng của shipper")
public class ShipmentHistoryShipperController {

    private final ShipmentHistoryShipperService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<ShipperShipmentListDto>>> list(
            @Valid ShipperShipmentSearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, searchRequest)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListResponse<ShipperShipmentDetailDto>>> getOrdersByShipmentId(
            @PathVariable Integer id,
            @Valid ShipperOrdersShipmentSearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ShipperShipmentDetailDto> result = service.getOrdersByShipmentId(userId, id,
                searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/export")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.SHIPMENT_EXPORT_LIST
    )
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            ShipperShipmentSearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, searchRequest);

        String fileName = "UTE Logistics_Báo cáo lịch sử chuyến hàng.xlsx";
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
    public ResponseEntity<byte[]> exportOrdersByShipmentId(
            HttpServletRequest request,
            @PathVariable Integer id,
            ShipperOrdersShipmentSearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.exportOrdersByShipmentId(userId, id, searchRequest);

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