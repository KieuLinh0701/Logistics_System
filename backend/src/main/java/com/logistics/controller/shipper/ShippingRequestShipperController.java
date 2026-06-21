package com.logistics.controller.shipper;

import com.logistics.audit.Audit;
import com.logistics.constants.AuditLogDescriptionConstant;
import com.logistics.entity.ShippingRequest;
import com.logistics.enums.AuditLogAction;
import com.logistics.enums.EntityType;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.ShippingRequestShipperService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipper/shipping-requests")
@Tag(name = "Shipper - Shipping Request", description = "Quản lý các yêu cầu hỗ trợ và khiếu nại được phân công cho nhân viên giao hàng")
public class ShippingRequestShipperController {

    @Autowired
    private ShippingRequestShipperService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingRequest>>> list() {
        return ResponseEntity.ok(ApiResponse.success(service.listForCurrentShipper()));
    }

    @PostMapping("/{id}/accept")
    @Audit(
            entity = EntityType.SHIPPING_REQUEST,
            action = AuditLogAction.UPDATE_STATUS,
            description = AuditLogDescriptionConstant.SHIPPING_REQUEST_UPDATE,
            params = {"id"}
    )
    public ResponseEntity<ApiResponse<Void>> accept(@PathVariable Integer id) {
        service.accept(id);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu đã được chấp nhận", null));
    }
}
