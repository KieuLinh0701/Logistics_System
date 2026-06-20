package com.logistics.controller.manager;

import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.dto.manager.order.ManagerOrderListDto;
import com.logistics.dto.manager.order.ManagerOrderStatusCountResponse;
import com.logistics.request.manager.order.ManagerOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
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
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.cancelOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> create(
            @RequestBody ManagerOrderCreateRequest managerOrderCreateRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.create(userId, managerOrderCreateRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Integer id,
            @RequestBody ManagerOrderCreateRequest orderRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.update(userId, id, orderRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/at-origin-office")
    public ResponseEntity<ApiResponse<Void>> setOrderAtOriginOffice(@PathVariable Integer id,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.setOrderAtOriginOffice(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmOrder(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.confirmOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
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
}