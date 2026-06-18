package com.logistics.controller.admin;

import com.logistics.response.ApiResponse;
import com.logistics.service.admin.OrderAdminService;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {

    @Autowired
    private OrderAdminService orderAdminService;

    private boolean isNotAdmin() {
        return !SecurityUtils.hasRole("admin");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(ApiResponse.success(orderAdminService.listOrders(page, limit, search, status)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteOrder(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        orderAdminService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đơn hàng thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagerOrderDetailDto>> getOrderDetail(@PathVariable Integer id) {
        if (isNotAdmin()) {
            return ResponseEntity.status(403).body(new ApiResponse<>(false, "Không có quyền truy cập", null));
        }

        return ResponseEntity.ok(ApiResponse.success(orderAdminService.getOrderById(id)));
    }
}
