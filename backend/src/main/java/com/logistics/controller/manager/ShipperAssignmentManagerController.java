package com.logistics.controller.manager;

import com.logistics.dto.manager.shipperAssignment.ManagerShipperAssignmentListDto;
import com.logistics.request.manager.shipperAssignment.ManagerShipperAssignmentEditRequest;
import com.logistics.request.manager.shipperAssignment.ManagerShipperAssignmentSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.ShipperAssignmentManagerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/manager/shipper-assignments")
@Tag(name = "Manager - Shipper Assignment", description = "Quản lý việc phân công khu vực/đơn hàng cho nhân viên giao hàng và xuất báo cáo")
public class ShipperAssignmentManagerController {

    @Autowired
    private ShipperAssignmentManagerService service;

    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(
            @RequestBody ManagerShipperAssignmentEditRequest editRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.create(userId, editRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id,
            @RequestBody ManagerShipperAssignmentEditRequest editRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.update(userId, id, editRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFutureAssignment(
            @PathVariable Long id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.deleteFutureAssignment(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<ManagerShipperAssignmentListDto>>> list(
            @Valid ManagerShipperAssignmentSearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerShipperAssignmentListDto> result = service.list(userId, searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(HttpServletRequest request,
            ManagerShipperAssignmentSearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.exportShipperAssignmentsExcel(userId, searchRequest);

        String fileName = "UTE Logistics_Báo cáo phân công giao hàng.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+",
                "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + fileName.replaceAll("\"", "") + "\"; filename*=UTF-8''" + encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}