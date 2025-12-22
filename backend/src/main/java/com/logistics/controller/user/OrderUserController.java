package com.logistics.controller.user;

import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.user.order.UserOrderDetailDto;
import com.logistics.dto.user.order.UserOrderListDto;
import com.logistics.request.user.order.UserOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.OrderCreateSuccess;
import com.logistics.service.user.OrderUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/orders")
public class OrderUserController {

    @Autowired
    private OrderUserService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<UserOrderListDto>>> list(
            @Valid UserOrderSearchRequest userOrderSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.list(userId, userOrderSearchRequest));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateSuccess>> create(
            @RequestBody UserOrderCreateRequest userOrderCreateRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.create(userId, userOrderCreateRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update( 
            @PathVariable Integer id,
            @RequestBody UserOrderCreateRequest userOrderCreateRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.updateOrder(userId, id, userOrderCreateRequest));
    }

    @GetMapping("/{trackingNumber}")
    public ResponseEntity<ApiResponse<UserOrderDetailDto>> getOrderByTrackingNumber(@PathVariable String trackingNumber,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.getOrderByTrackingNumber(userId, trackingNumber));
    }

    @GetMapping("/id/{id}") 
    public ResponseEntity<ApiResponse<UserOrderDetailDto>> getOrderById(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.getOrderById(userId, id));
    }

    @PatchMapping("/{id}/public")
    public ResponseEntity<ApiResponse<String>> publicOrder(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.publicOrder(userId, id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelOrder(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.cancelOrder(userId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.deleteOrder(userId, id));
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

        return ResponseEntity.ok(service.getOrdersForPrint(userId, orderIds));
    }

    @PatchMapping("/{id}/ready")
    public ResponseEntity<ApiResponse<Boolean>> setOrderReadyForPickup(@PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(service.setOrderReadyForPickup(userId, id));
    }

}