package com.logistics.controller.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.shipment.ManagerOrdersShipmentSearchRequest;
import com.logistics.request.manager.shipment.ManagerShipmentAddEditRequest;
import com.logistics.request.manager.shipment.ManagerShipmentSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.ShipmentManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/shipments")
public class ShipmentManagerController {

    @Autowired
    private ShipmentManagerService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<ManagerShipmentListDto>>> list(
            @Valid ManagerShipmentSearchRequest managerShipmentSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.list(userId, managerShipmentSearchRequest));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListResponse<ManagerShipmentDetailDto>>> getOrdersByShipmentId(
            @PathVariable Integer id,
            @Valid ManagerOrdersShipmentSearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<ListResponse<ManagerShipmentDetailDto>> result = service.getOrdersByShipmentId(userId, id,
                searchRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/export")
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

        ApiResponse<ListResponse<ManagerShipmentListDto>> result = service.getPendingShipments(userId, searchRequest);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelShipment(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.cancelShipment(userId, id));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(
            @RequestBody ManagerShipmentAddEditRequest editRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.create(userId, editRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(
            @PathVariable Integer id,
            @RequestBody ManagerShipmentAddEditRequest editRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.update(userId, id, editRequest));
    }
}