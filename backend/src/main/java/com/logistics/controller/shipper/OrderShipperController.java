package com.logistics.controller.shipper;

import com.logistics.request.shipper.UpdateDeliveryStatusRequest;
import com.logistics.request.shipper.PickedUpRequest;
import com.logistics.request.shipper.DeliverOriginRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.OrderShipperService;
import com.logistics.utils.SecurityUtils;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/shipper")
public class OrderShipperController {

    @Autowired
    private OrderShipperService shipperService;

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
    public ResponseEntity<ApiResponse<String>> claimOrder(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.claimOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Nhận đơn thành công"));
    }

    @PostMapping("/orders/{id}/claim-request")
    public ResponseEntity<ApiResponse<String>> claimOrderRequest(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.claimOrderRequest(id);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu nhận đơn thành công"));
    }

    @PostMapping("/orders/{id}/unclaim")
    public ResponseEntity<ApiResponse<String>> unclaimOrder(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.unclaimOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Hủy nhận đơn thành công"));
    }

    @PutMapping("/orders/{id}/status")
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
    public ResponseEntity<ApiResponse<String>> markPickedUp(@PathVariable Integer id, @RequestBody PickedUpRequest request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.markPickedUp(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận lấy hàng"));
    }

    @GetMapping("/orders/{id}/partial-start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startPartialDelivery(@PathVariable Integer id) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        return ResponseEntity.ok(ApiResponse.success(shipperService.startPartialDelivery(id)));
    }

    @PostMapping("/order-products/{orderProductId}/delivered")
    public ResponseEntity<ApiResponse<String>> markProductDelivered(@PathVariable Integer orderProductId, @RequestBody Map<String, Object> payload) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        Integer qty = payload.get("deliveredQuantity") instanceof Number ? ((Number) payload.get("deliveredQuantity")).intValue() : null;
        shipperService.markProductDeliveredAtomic(orderProductId, qty);
        return ResponseEntity.ok(ApiResponse.success("Đã giao sản phẩm"));
    }

    @PostMapping("/order-products/{orderProductId}/returned")
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
    public ResponseEntity<ApiResponse<String>> deliverToOrigin(@PathVariable Integer id, @RequestBody(required = false) DeliverOriginRequest request) {
        if (isNotShipper()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }
        shipperService.deliverToOrigin(id, request);
        return ResponseEntity.ok(ApiResponse.success("Đã trả hàng về kho gốc"));
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
        Integer routeId = (Integer) request.get("routeId");
        shipperService.startRoute(routeId);
        return ResponseEntity.ok(ApiResponse.success("Đã bắt đầu lộ trình"));
    }
}
