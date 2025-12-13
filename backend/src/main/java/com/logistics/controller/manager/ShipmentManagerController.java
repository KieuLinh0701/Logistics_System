package com.logistics.controller.manager;

import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.dto.manager.order.ManagerOrderListDto;
import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestDetailDto;
import com.logistics.request.manager.order.ManagerOrderCreateRequest;
import com.logistics.request.manager.shipment.ManagerOrdersShipmentSearchRequest;
import com.logistics.request.manager.shipment.ManagerShipmentSearchRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.OrderManagerService;
import com.logistics.service.manager.ShipmentManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.List;

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

        ApiResponse<ListResponse<ManagerShipmentDetailDto>> result = service.getOrdersByShipmentId(userId, id, searchRequest);
        return ResponseEntity.ok(result);
    }
}