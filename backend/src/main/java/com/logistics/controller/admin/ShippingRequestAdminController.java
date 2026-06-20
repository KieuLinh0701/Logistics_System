package com.logistics.controller.admin;

import com.logistics.entity.ShippingRequest;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.ShippingRequestAdminService;
import com.logistics.service.common.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/shipping-requests")
@Tag(name = "Admin - Shipping Request", description = "Quản lý yêu cầu vận chuyển")
public class ShippingRequestAdminController {
    @Autowired
    private ShippingRequestAdminService shippingRequestAdminService;
    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        return ResponseEntity.ok(ApiResponse.success(shippingRequestAdminService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingRequest>> detail(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(shippingRequestAdminService.detail(id)));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<String>> assignOffice(@PathVariable Integer id, @RequestParam Integer officeId) {
        shippingRequestAdminService.assignOffice(id, officeId);
        return ResponseEntity.ok(ApiResponse.success("Đã phân công cho bưu cục"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(@PathVariable Integer id, @RequestParam ShippingRequestStatus status) {
        shippingRequestAdminService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái"));
    }
}
