package com.logistics.controller.manager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.manager.employee.ManagerEmployeeListDto;
import com.logistics.dto.manager.employee.ManagerEmployeeListWithShipperAssignmentDto;
import com.logistics.dto.manager.employee.ManagerEmployeePerformanceDto;
import com.logistics.dto.manager.shipment.ManagerShipmentPerformanceDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.employee.ManagerEmployeeEditRequest;
import com.logistics.request.manager.employee.ManagerEmployeeSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.EmployeeManagerService;
import com.logistics.service.manager.ShipmentManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/manager/employees")
public class EmployeeManagerController {

    @Autowired
    private EmployeeManagerService service;

    @Autowired
    private ShipmentManagerService shipmentManagerService;

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeeListDto>>> list(
            @Valid ManagerEmployeeSearchRequest managerShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<ListResponse<ManagerEmployeeListDto>> result = service.list(userId,
                managerShippingRequestSearchRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/performance")
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeePerformanceDto>>> getEmployeePerformance(
            @Valid SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<ListResponse<ManagerEmployeePerformanceDto>> result = service.getEmployeePerformance(userId,
                searchRequest);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(
            @RequestBody ManagerEmployeeEditRequest managerEmployeeEditRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.createEmployee(userId, managerEmployeeEditRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Integer id,
            @RequestBody ManagerEmployeeEditRequest managerEmployeeEditRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.updateEmployee(userId, id, managerEmployeeEditRequest));
    }

    @GetMapping("/shippers/active/with-assignments")
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeeListWithShipperAssignmentDto>>> getActiveShippersWithActiveAssignments(
            @ModelAttribute ManagerEmployeeSearchRequest managerShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<ListResponse<ManagerEmployeeListWithShipperAssignmentDto>> result = service
                .getActiveShippersWithActiveAssignments(userId,
                        managerShippingRequestSearchRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/shippers/active")
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeeListDto>>> getActiveShippers(
            @ModelAttribute ManagerEmployeeSearchRequest managerShippingRequestSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<ListResponse<ManagerEmployeeListDto>> result = service.getActiveShippers(userId,
                managerShippingRequestSearchRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/shipment-type")
    public ResponseEntity<ApiResponse<ListResponse<ManagerEmployeeListDto>>> getActiveEmployeesByShipmentType(
            @Valid SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        ApiResponse<ListResponse<ManagerEmployeeListDto>> result = service.getActiveEmployeesByShipmentType(userId,
                searchRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/shipments")
    public ResponseEntity<ApiResponse<ListResponse<ManagerShipmentPerformanceDto>>> getShipmentsByEmployeeId(
            @Valid SearchRequest searchRequest,
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(shipmentManagerService.getShipmentsByEmployeeId(userId, id, searchRequest));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPerformance(HttpServletRequest request,
            SearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.exportPerformance(userId, searchRequest);

        String fileName = "UTE Logistics_Báo cáo hiệu suất nhân viên.xlsx";
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