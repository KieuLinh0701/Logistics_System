package com.logistics.controller.user;

import com.logistics.dto.OrderDto;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.user.OrderUserService;
import com.logistics.utils.SecurityUtils;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/orders")
public class OrderUserController {

    @Autowired
    private OrderUserService service;

    private boolean isNotPermitRole() {
        return !SecurityUtils.hasRole("user");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<OrderDto>>> list(@Valid UserOrderSearchRequest request) {
        if (isNotPermitRole()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        Integer userId;
        try {
            userId = SecurityUtils.getAuthenticatedUserId();
        } catch (RuntimeException e) {
            ApiResponse<ListResponse<OrderDto>> response = new ApiResponse<>(false, e.getMessage(), null);
            return ResponseEntity.status(401).body(response);
        }

        return ResponseEntity.ok(service.list(userId, request));
    }
}


