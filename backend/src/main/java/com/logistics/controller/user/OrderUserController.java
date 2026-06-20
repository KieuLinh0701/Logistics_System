package com.logistics.controller.user;

import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.user.order.UserOrderDetailDto;
import com.logistics.dto.user.order.UserOrderListDto;
import com.logistics.dto.user.order.UserOrderStatusCountResponse;
import com.logistics.request.user.order.UserOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.OrderCreateSuccess;
import com.logistics.service.user.OrderUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/user/orders")
@Tag(name = "User - Order", description = "Quản lý đơn hàng của người dùng: tạo mới, cập nhật, theo dõi lộ trình, in ấn và xuất báo cáo")
public class OrderUserController {

    @Autowired
    private OrderUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<UserOrderListDto>>> list(
            @Valid UserOrderSearchRequest userOrderSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, userOrderSearchRequest)));
    }

    @GetMapping("/status-counts")
    public ResponseEntity<ApiResponse<List<UserOrderStatusCountResponse>>> getStatusCounts(
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

    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateSuccess>> create(
            @Valid @RequestBody UserOrderCreateRequest userOrderCreateRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.create(userId, userOrderCreateRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UserOrderCreateRequest userOrderCreateRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.updateOrder(userId, id, userOrderCreateRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<ApiResponse<UserOrderDetailDto>> getOrderByTrackingNumber(
            @PathVariable String trackingNumber,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getOrderByTrackingNumber(userId, trackingNumber)));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<UserOrderDetailDto>> getOrderById(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getOrderById(userId, id)));
    }

    @PatchMapping("/{id}/public")
    public ResponseEntity<ApiResponse<String>> publicOrder(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.publicOrder(userId, id)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelOrder(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.cancelOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.deleteOrder(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
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

    @PatchMapping("/{id}/ready")
    public ResponseEntity<ApiResponse<Void>> setOrderReadyForPickup(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.setOrderReadyForPickup(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            UserOrderSearchRequest userOrderSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, userOrderSearchRequest);

        String fileName = "UTE Logistics_Báo cáo đơn hàng.xlsx";
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