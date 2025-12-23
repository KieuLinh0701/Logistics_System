package com.logistics.controller.admin;

import com.logistics.entity.Office;
import com.logistics.entity.ShippingRequest;
import com.logistics.enums.ShippingRequestStatus;
import com.logistics.response.ApiResponse;
import com.logistics.service.admin.ShippingRequestAdminService;
import com.logistics.service.common.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shipping-requests")
public class ShippingRequestAdminController {
    @Autowired
    private ShippingRequestAdminService shippingRequestAdminService;
    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<java.util.Map<String, Object>>>> list() {
        return ResponseEntity.ok(shippingRequestAdminService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShippingRequest>> detail(@PathVariable Integer id) {
        return ResponseEntity.ok(shippingRequestAdminService.detail(id));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<String>> assignOffice(@PathVariable Integer id, @RequestParam Integer officeId) {
        return ResponseEntity.ok(shippingRequestAdminService.assignOffice(id, officeId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(@PathVariable Integer id, @RequestParam ShippingRequestStatus status) {
        return ResponseEntity.ok(shippingRequestAdminService.updateStatus(id, status));
    }
}
