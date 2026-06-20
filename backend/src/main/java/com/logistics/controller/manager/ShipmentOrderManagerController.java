package com.logistics.controller.manager;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.manager.shipmentOrder.SaveShipmentOrdersRequest;
import com.logistics.response.BulkResponse;
import com.logistics.service.manager.ShipmentOrderManagerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/manager/shipment-orders")
@Tag(name = "Manager - Shipment Order", description = "Quản lý chi tiết danh sách đơn hàng được gán vào chuyến hàng tại bưu cục")
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
        @Audit(
                entity = EntityType.SHIPMENT_ORDER,
                action = AuditLogAction.CREATE,
                description = AuditLogDescriptionConstant.SHIPMENT_ORDER_SAVE,
                params = {"id"}
        )
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