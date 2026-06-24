package com.logistics.controller.manager;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.dto.manager.order.ManagerOrderListDto;
import com.logistics.dto.manager.order.ManagerOrderStatusCountResponse;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.request.manager.order.ManagerOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.request.user.order.UserUrgentOrderSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.manager.order.UrgentOrderResponse;
import com.logistics.service.manager.OrderManagerService;
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
import java.util.Arrays;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/orders")
@Tag(name = "Manager - Order", description = "Quản lý đơn hàng, cập nhật trạng thái và xuất báo cáo tại bưu cục")
public class OrderManagerController {

    private final OrderManagerService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<ManagerOrderListDto>>> list(
            @Valid UserOrderSearchRequest userOrderSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, userOrderSearchRequest)));
    }

    @GetMapping("/status-counts")
    public ResponseEntity<ApiResponse<List<ManagerOrderStatusCountResponse>>> getStatusCounts(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.getStatusCounts(userId)));
    }

    @GetMapping("/all-ids")
    public ResponseEntity<ApiResponse<List<Integer>>> getAllOrderIds(
            @Valid UserOrderSearchRequest userOrderSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getAllOrderIds(userId, userOrderSearchRequest)));
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ApiResponse<ManagerOrderDetailDto>> getOrderByTrackingNumber(
            @PathVariable String trackingNumber,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getOrderByTrackingNumber(userId, trackingNumber)));
    }

    @GetMapping("/print")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.PRINT,
            description = AuditLogDescriptionConstant.ORDER_PRINT,
            params = {"orderIds"}
    )
    public ResponseEntity<ApiResponse<List<OrderPrintDto>>> getOrdersForPrint(
            @RequestParam(name = "orderIds") String orderIdsStr,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        List<Integer> orderIds = Arrays.stream(orderIdsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(service.getOrdersForPrint(userId, orderIds)));
    }

    @PatchMapping("/{id}/cancel")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.CANCEL,
            description = AuditLogDescriptionConstant.ORDER_CANCEL,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.cancelOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.CREATE,
            description = AuditLogDescriptionConstant.ORDER_CREATE
    )
    public ResponseEntity<ApiResponse<String>> create(
            @RequestBody ManagerOrderCreateRequest managerOrderCreateRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.create(userId, managerOrderCreateRequest)));
    }

    @PutMapping("/{id}")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE,
            description = AuditLogDescriptionConstant.ORDER_UPDATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Integer id,
            @RequestBody ManagerOrderCreateRequest orderRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.update(userId, id, orderRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/at-origin-office")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_SET_AT_ORIGIN_OFFICE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> setOrderAtOriginOffice(@PathVariable Integer id,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.setOrderAtOriginOffice(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/confirm")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_CONFIRM,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> confirmOrder(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.confirmOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/returned")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_RETURNED,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> returnedOrder(@PathVariable Integer id,
                                                          HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.setOrderReturned(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.ORDER_EXPORT
    )
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            UserOrderSearchRequest userOrderSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, userOrderSearchRequest);

        String fileName = "UTE Logistics_Báo cáo đơn hàng bưu cục.xlsx";
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

    // Danh sách đơn URGENT thuộc cityCode của office manager
    @GetMapping("/urgent-pickup")
    public ResponseEntity<ApiResponse<ListResponse<UrgentOrderResponse>>> getUrgentOrders(
            HttpServletRequest request,
            UserUrgentOrderSearchRequest userUrgentOrderSearchRequest) {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.getUrgentOrders(userId, userUrgentOrderSearchRequest)));
    }

    @GetMapping("/urgent-pickup/export")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.EXPORT,
            description = AuditLogDescriptionConstant.ORDER_URGENT_EXPORT
    )
    public ResponseEntity<byte[]> exportUrgent(
            HttpServletRequest request,
            UserUrgentOrderSearchRequest userUrgentOrderSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.exportUrgent(userId, userUrgentOrderSearchRequest);

        String fileName = "UTE Logistics_Báo cáo xử lý các yêu cầu lấy đơn hàng bưu cục.xlsx";
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

    @PatchMapping("/urgent-pickup/{id}/confirm")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_URGENT_CONFIRM,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> confirmUrgentOrder(@PathVariable Integer id,
                                                          HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.confirmUrgentOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Lấy danh sách đơn chờ xác nhận đến bưu cục đích
    @GetMapping("/pending-destination-confirm")
    public ResponseEntity<ApiResponse<ListResponse<ManagerOrderListDto>>> getPendingDestinationConfirmOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.success(service.getPendingDestinationConfirmOrders(userId, page, limit)));
    }

    // Stage 2 - Xác nhận đơn đã đến bưu cục đích
    @PatchMapping("/{id}/confirm-destination")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.ORDER_CONFIRM_DESTINATION,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> confirmDestinationOffice(@PathVariable Integer id,
                                                                     HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.confirmDestinationOffice(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 2. Manager claim đơn về office của mình
//    @PostMapping("/{trackingNumber}/claim")
//
//    public ResponseEntity<?> claimOrder(
//            @PathVariable String trackingNumber,
//            HttpServletRequest request) throws Exception {
//
//        Integer userId = (Integer) request.getAttribute("currentUserId");
//        urgentPickupService.claimOrder(trackingNumber, userId);
//        return ResponseEntity.ok(Map.of("message", "Đã nhận đơn về bưu cục"));
//    }

    // 3. Load shipper available + shipment IN_TRANSIT hiện tại (nếu có)
//    @GetMapping("/{orderId}/shippers")
//    public ResponseEntity<List<ShipperWithShipmentResponse>> getAvailableShippers(
//            @PathVariable Integer orderId,
//            @RequestAttribute("employeeId") Integer employeeId
//    ) {
//        return ResponseEntity.ok(urgentPickupService.getAvailableShippers(orderId, employeeId));
//    }

    // 4. Assign shipper: add vào shipment IN_TRANSIT hoặc tạo mới PENDING → CONFIRMED
//    @PostMapping("/{trackingNumber}/assign")
//    public ResponseEntity<?> assignShipper(
//            @PathVariable String trackingNumber,
//            @RequestBody AssignShipperRequest assignShipperRequest,
//            HttpServletRequest request,
//            UserOrderSearchRequest userOrderSearchRequest) throws Exception {
//
//        Integer userId = (Integer) request.getAttribute("currentUserId");
//        urgentPickupService.assignShipper(orderId, request.getShipperId(), request.getShipmentId(), employeeId);
//        return ResponseEntity.ok(Map.of("message", "Đã assign shipper thành công"));
//    }
}