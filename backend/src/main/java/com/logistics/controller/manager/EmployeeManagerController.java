package com.logistics.controller.manager;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.dto.manager.employee.ManagerEmployeeListWithShipperAssignmentDto;
import com.logistics.dto.manager.employee.ManagerEmployeePerformanceDto;
import com.logistics.dto.manager.shipment.ManagerShipmentPerformanceDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.employee.ManagerEmployeeEditRequest;
import com.logistics.request.manager.employee.ManagerEmployeeSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.EmployeeManagerService;
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
@RequestMapping("/api/manager/employees")
@Tag(name = "Manager - Employee", description = "Quản lý nhân sự, theo dõi hiệu suất và xuất báo cáo vận hành")
public class EmployeeManagerController {

    private final EmployeeManagerService service;
    private final ShipmentManagerService shipmentManagerService;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeeListDto>>> list(
            @Valid ManagerEmployeeSearchRequest managerShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerEmployeeListDto> result = service.list(userId,
                managerShippingRequestSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeePerformanceDto>>> getEmployeePerformance(
            @Valid SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerEmployeePerformanceDto> result = service.getEmployeePerformance(userId,
                searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @Audit(
            entity = EntityType.EMPLOYEE,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.EMPLOYEE_CREATE
    )
    public ResponseEntity<ApiResponse<String>> create(
            @RequestBody ManagerEmployeeEditRequest managerEmployeeEditRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        String message = service.createEmployee(userId, managerEmployeeEditRequest);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @PutMapping("/{id}")
    @Audit(
            entity = EntityType.EMPLOYEE,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.EMPLOYEE_UPDATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Integer id,
                                                       @RequestBody ManagerEmployeeEditRequest managerEmployeeEditRequest,
                                                       HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        String message = service.updateEmployee(userId, id, managerEmployeeEditRequest);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @GetMapping("/shippers/active/with-assignments")
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeeListWithShipperAssignmentDto>>> getActiveShippersWithActiveAssignments(
            @ModelAttribute ManagerEmployeeSearchRequest managerShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerEmployeeListWithShipperAssignmentDto> result = service
                .getActiveShippersWithActiveAssignments(userId,
                        managerShippingRequestSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/shippers/active")
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeeListDto>>> getActiveShippers(
            @ModelAttribute ManagerEmployeeSearchRequest managerShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerEmployeeListDto> result = service.getActiveShippers(userId,
                managerShippingRequestSearchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/shipment-type")
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeeListDto>>> getActiveEmployeesByShipmentType(
            @Valid SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        ListResponse<ManagerEmployeeListDto> result = service.getActiveEmployeesByShipmentType(userId,
                searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/shipments")
    public ResponseEntity<ApiResponse<ListResponse<ManagerShipmentPerformanceDto>>> getShipmentsByEmployeeId(
            @Valid SearchRequest searchRequest,
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(shipmentManagerService.getShipmentsByEmployeeId(userId, id, searchRequest)));
    }

    @GetMapping("/{id}/shipments/export")
    @Audit(
            entity = EntityType.EMPLOYEE,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.EMPLOYEE_EXPORT_SHIPMENTS,
            params = {"id"}
    )
    public ResponseEntity<byte[]> export(
            @PathVariable Integer id,
            HttpServletRequest request,
            SearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = shipmentManagerService.exportShipmentsByEmployeeId(userId, id, searchRequest);

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

    @GetMapping("/performance/export")
    @Audit(
            entity = EntityType.EMPLOYEE,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.EMPLOYEE_EXPORT_PERFORMANCE
    )
    public ResponseEntity<byte[]> exportPerformance(HttpServletRequest request,
                                                    SearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.exportPerformance(userId, searchRequest);

        String fileName = "UTE Logistics_Báo cáo hiệu suất giao hàng của nhân viên.xlsx";
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

    @GetMapping("/export")
    @Audit(
            entity = EntityType.EMPLOYEE,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.EMPLOYEE_EXPORT
    )
    public ResponseEntity<byte[]> export(HttpServletRequest request,
                                         ManagerEmployeeSearchRequest managerEmployeeSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, managerEmployeeSearchRequest);

        String fileName = "UTE Logistics_Báo cáo danh sách nhân viên.xlsx";
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

    @GetMapping("/shipper-assignments/export")
    @Audit(
            entity = EntityType.EMPLOYEE,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.EMPLOYEE_EXPORT_ASSIGNMENTS
    )
    public ResponseEntity<byte[]> exportActiveShippersWithActiveAssignments(
            HttpServletRequest request,
            ManagerEmployeeSearchRequest managerShippingRequestSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.exportActiveShippersWithActiveAssignments(userId, managerShippingRequestSearchRequest);

        String fileName = "UTE Logistics_Báo cáo danh sách nhân viên giao hàng và khu vực phân công.xlsx";
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