package com.logistics.controller.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.shipment.ManagerOrdersShipmentSearchRequest;
import com.logistics.request.manager.shipment.ManagerShipmentAddEditRequest;
import com.logistics.request.manager.shipment.ManagerShipmentSearchRequest;
import com.logistics.request.manager.shipperAssignment.ManagerShipperAssignmentEditRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.ShipmentManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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