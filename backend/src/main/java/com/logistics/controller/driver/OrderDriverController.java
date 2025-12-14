package com.logistics.controller.driver;

import com.logistics.request.driver.PickUpRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.driver.OrderDriverService;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/driver/orders")
public class OrderDriverController {

    @Autowired
    private OrderDriverService orderDriverService;

    private boolean isNotDriver() {
        return !SecurityUtils.hasRole("driver");
    }

    @GetMapping("/context")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContext() {
        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(orderDriverService.getContext());
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(orderDriverService.getPendingOrders(page, limit));
    }

    @PostMapping("/pickup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pickUp(@RequestBody PickUpRequest request) {
        if (isNotDriver()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }
        return ResponseEntity.ok(orderDriverService.pickUp(request));
    }
}




