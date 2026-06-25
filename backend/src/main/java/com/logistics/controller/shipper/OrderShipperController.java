package com.logistics.controller.shipper;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.request.shipper.*;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.OrderShipperService;
import com.logistics.service.shipper.ShipmentDeliveryService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/shipper")
@Tag(name = "Shipper - Order", description = "Quản lý đơn hàng, quy trình giao nhận, xử lý sự cố và lộ trình cho nhân viên giao hàng")
public class OrderShipperController {

    @Autowired
    private OrderShipperService shipperService;

    @Autowired
    private ShipmentDeliveryService shipmentDeliveryService;

    private boolean isNotShipper() {
        return !SecurityUtils.hasRole("shipper");
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.getDashboard()));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(shipperService.listOrders(page, limit, status, search)));
    }

    @GetMapping("/orders-unassigned")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listUnassignedOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(shipperService.listUnassignedOrders(page, limit)));
    }

    @GetMapping("/pickup-requests")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listPickupRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(shipperService.listPickupByCourierRequests(page, limit)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrder(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.getOrderById(id)));
    }

    @GetMapping("/orders/tracking/{trackingNumber}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrderByTrackingNumber(@PathVariable String trackingNumber) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.getOrderByTrackingNumber(trackingNumber)));
    }

    @PostMapping("/orders/{id}/claim")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_CLAIM,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> claimOrder(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.claimOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Nhận đơn thành công"));
    }

    @PostMapping("/orders/{id}/accept-pickup")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_CLAIM_REQUEST,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptPickupRequest(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        Map<String, Object> result = shipmentDeliveryService.acceptPickupRequest(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/orders/{id}/start-pickup")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_PICKED_UP,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> startPickup(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        // start pickup: set PICKING_UP - yêu cầu đơn thuộc shipment IN_TRANSIT
        shipmentDeliveryService.startPickup(id);
        return ResponseEntity.ok(ApiResponse.success("Đã bắt đầu đi lấy hàng"));
    }

    @PostMapping("/orders/{id}/unclaim")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_UNCLAIM,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> unclaimOrder(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.unclaimOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Hủy nhận đơn thành công"));
    }

    @PutMapping("/orders/{id}/status")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_DELIVERY_SUCCESS,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> updateDeliveryStatus(
            @PathVariable Integer id,
            @RequestBody UpdateDeliveryStatusRequest request) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        shipperService.updateDeliveryStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công"));
    }

    @PostMapping("/orders/{id}/delivery-attempt")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_DELIVERY_ATTEMPT,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> createDeliveryAttempt(
            @PathVariable Integer id,
            @RequestBody UpdateDeliveryStatusRequest request) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        shipperService.recordDeliveryAttempt(id, request);
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận lần giao thành công"));
    }

    @PostMapping("/orders/{id}/return-failed-to-office")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_RETURN_FAILED,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> returnFailedToOffice(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        shipperService.returnFailedToOffice(id);
        return ResponseEntity.ok(ApiResponse.success("Trả hàng về bưu cục thành công"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeliveryHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(shipperService.getDeliveryHistory(page, limit, status)));
    }

    @PostMapping(value = "/incident", consumes = {"multipart/form-data"})
    @Audit(
            entity = EntityType.INCIDENT_REPORT,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.INCIDENT_REPORT_CREATE,
            params = {"orderId"}
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> createIncident(
            @RequestParam Integer orderId,
            @RequestParam(required = false) String incidentType,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) MultipartFile[] images) {

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(shipperService.createIncidentReport(orderId, incidentType, title, description, priority, images)));
    }

    @PostMapping("/orders/{id}/picked-up")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_PICKED_UP,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> markPickedUp(@PathVariable Integer id, @RequestBody PickedUpRequest request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        boolean alreadyPickedUp = shipperService.markPickedUp(id, request);
        String message = alreadyPickedUp
                ? "Đơn đã được lấy hàng trước đó"
                : "Đã xác nhận lấy hàng";
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PostMapping("/orders/{id}/retry-pickup")
    public ResponseEntity<ApiResponse<String>> retryPickup(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.retryPickup(id);
        return ResponseEntity.ok(ApiResponse.success("Đã tiến hành đến lấy lại"));
    }

    @GetMapping("/orders/{id}/partial-start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startPartialDelivery(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.startPartialDelivery(id)));
    }

    @PostMapping("/order-products/{orderProductId}/delivered")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.ORDER_PRODUCT_DELIVERED,
            params = {"orderProductId"}
    )
    public ResponseEntity<ApiResponse<String>> markProductDelivered(@PathVariable Integer orderProductId, @RequestBody Map<String, Object> payload) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        Integer qty = payload.get("deliveredQuantity") instanceof Number ? ((Number) payload.get("deliveredQuantity")).intValue() : null;
        shipperService.markProductDeliveredAtomic(orderProductId, qty);
        return ResponseEntity.ok(ApiResponse.success("Đã giao sản phẩm"));
    }

    @PostMapping("/order-products/{orderProductId}/returned")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.ORDER_PRODUCT_RETURNED,
            params = {"orderProductId"}
    )
    public ResponseEntity<ApiResponse<String>> markProductReturned(@PathVariable Integer orderProductId, @RequestBody Map<String, Object> payload) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        Integer qty = payload.get("returnedQuantity") instanceof Number ? ((Number) payload.get("returnedQuantity")).intValue() : null;
        String reason = payload.get("reason") != null ? String.valueOf(payload.get("reason")) : null;
        shipperService.markProductReturnedAtomic(orderProductId, qty, reason);
        return ResponseEntity.ok(ApiResponse.success("Đã trả sản phẩm"));
    }

    @PostMapping("/orders/{id}/partial-finish")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_PARTIAL_DELIVERY,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> finishPartialDelivery(@PathVariable Integer id) {
        try {
            String role = Objects.requireNonNull(SecurityUtils.getAuthenticatedUserRole())
                    .getName();
            Integer userId = SecurityUtils.getAuthenticatedUserId();
        } catch (Exception e) {
        }

        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.finishPartialDelivery(id);
        return ResponseEntity.ok(ApiResponse.success("Hoàn tất giao một phần"));
    }

    @PostMapping("/orders/{id}/deliver-origin")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_RETURN_TO_ORIGIN,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> deliverToOrigin(@PathVariable Integer id, @RequestBody(required = false) DeliverOriginRequest request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.deliverToOrigin(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã trả hàng về kho gốc"));
    }

    // Lấy danh sách đơn chờ xác nhận đến bưu cục đích
    @GetMapping("/orders/pending-destination-confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingDestinationConfirmOrders() {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.getPendingDestinationConfirmOrders()));
    }

    // Stage 2 - Xác nhận đơn đã đến bưu cục đích
    @PatchMapping("/orders/{id}/confirm-destination")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_CONFIRM_DESTINATION,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> confirmDestinationOffice(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.confirmDestinationOffice(id);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đơn đã đến bưu cục đích thành công"));
    }

    @GetMapping("/incidents")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listIncidents() {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(shipperService.listIncidentReports()));
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getIncident(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(shipperService.getIncidentDetail(id)));
    }

    @GetMapping("/route")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeliveryRoute() {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.getDeliveryRoute()));
    }

    @PostMapping("/route/start")
    public ResponseEntity<ApiResponse<String>> startRoute(@RequestBody Map<String, Object> request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        // Theo rule mới: routeId là AiRoutePlanRoute.id (Long), KHÔNG phải Shipment.id.
        // Endpoint này KHÔNG còn map routeId -> shipmentId. Frontend phải dùng
        // /shipments/{id}/start với shipmentId.
        Integer shipmentId = request.get("shipmentId") instanceof Number ? ((Number) request.get("shipmentId")).intValue() : null;
        if (shipmentId == null) {
            throw new AppException(com.logistics.exception.enums.ShipmentErrorCode.SHIPMENT_NOT_FOUND,
                    "Vui lòng gọi /shipments/{id}/start với shipmentId. routeId không phải shipmentId.");
        }
        shipmentDeliveryService.startShipment(shipmentId);
        return ResponseEntity.ok(ApiResponse.success("Đã bắt đầu chuyến giao hàng"));
    }

    @PostMapping("/shipments/{id}/start")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.UPDATE_STATUS,
            description = "Bắt đầu chuyến DELIVERY (shipper)",
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> startDeliveryShipment(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipmentDeliveryService.startShipment(id);
        return ResponseEntity.ok(ApiResponse.success("Đã bắt đầu chuyến giao hàng"));
    }

    @PostMapping("/shipments/{id}/finish")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.UPDATE_STATUS,
            description = "Hoàn tất chuyến DELIVERY (shipper)",
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> finishDeliveryShipment(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipmentDeliveryService.finishShipment(id);
        return ResponseEntity.ok(ApiResponse.success("Đã hoàn tất chuyến giao hàng"));
    }

    @PostMapping("/shipments/{id}/start-delivery-all")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.UPDATE_STATUS,
            description = "Bulk chuyển PICKED_UP -> DELIVERING cho toàn bộ order trong chuyến",
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> startDeliveryAllForShipment(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipmentDeliveryService.startDeliveryAll(id)));
    }

    @GetMapping("/shipments/active")
    public ResponseEntity<ApiResponse<List<com.logistics.entity.Shipment>>> listActiveDeliveryShipments() {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipmentDeliveryService.listActiveShipmentsForCurrentShipper()));
    }

    @PostMapping("/route/re-optimize")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reOptimizeRoute(
            @RequestBody ShipperReOptimizeRequest body) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.reOptimizeRoute(body)));
    }

    @PostMapping("/route/pickup-insert")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pickupInsertion(
            @RequestBody PickupInsertionRequest body) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.assignPickupToShipperRoute(body)));
    }

    // ==================== Phase 3C: Pickup insert into Shipment ====================

    /**
     * Pickup insert trực tiếp vào Shipment (ShipmentOrder là source of truth).
     * Frontend dùng endpoint này khi routeInfo.source === "SHIPMENT".
     * Old /route/pickup-insert (PickupInsertionRequest) giữ lại cho AI-source fallback.
     */
    @PostMapping("/shipments/{id}/pickup-insert")
    @Audit(
            entity = EntityType.SHIPMENT,
            action = AuditLogAction.UPDATE,
            description = "Thêm đơn pickup vào chuyến DELIVERY (shipper)",
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> insertPickupIntoShipment(
            @PathVariable Integer id,
            @RequestBody InsertPickupShipmentRequest body) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.insertPickupIntoShipment(id, body)));
    }
}
