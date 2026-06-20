package com.logistics.controller.admin;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.OrderAdminService;
import com.logistics.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@Tag(name = "Admin - Order", description = "Quản lý đơn hàng")
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
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(orderAdminService.listOrders(page, limit, search, status)));
    }

    @DeleteMapping("/{id}")
    @Audit(
            entity = EntityType.ORDER,
            action = AuditLogAction.DELETE,
            description = AuditLogDescriptionConstant.ORDER_DELETE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<String>> deleteOrder(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        orderAdminService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa đơn hàng thành công"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagerOrderDetailDto>> getOrderDetail(@PathVariable Integer id) {
        if (isNotAdmin()) {
            throw new AppException(CommonErrorCode.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success(orderAdminService.getOrderById(id)));
    }
}
