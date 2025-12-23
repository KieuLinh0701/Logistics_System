package com.logistics.controller.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.request.manager.shipmentOrder.SaveShipmentOrdersRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.BulkResponse;
import com.logistics.service.manager.ShipmentOrderManagerService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/shipment-orders")
public class ShipmentOrderManagerController {

    @Autowired
    private ShipmentOrderManagerService service;

    @GetMapping("/{id}/check")
    public ResponseEntity<BulkResponse<ManagerShipmentDetailDto>> checkOrderForShipment(
            @PathVariable("id") Integer id,
            @RequestParam("trackingNumber") String trackingNumber,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        List<String> trackingNumbers = Arrays.stream(trackingNumber.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        return ResponseEntity.ok(service.checkOrdersForShipment(userId, id, trackingNumbers));
    }

    @PostMapping("/{id}/save-orders")
    public ResponseEntity<BulkResponse<String>> saveShipmentOrders(
            @PathVariable Integer id,
            @RequestBody SaveShipmentOrdersRequest requestBody,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(
                service.saveShipmentOrders(
                        userId,
                        id,
                        requestBody.getRemovedOrderIds(),
                        requestBody.getAddedOrderIds()));
    }

}