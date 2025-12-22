package com.logistics.controller.shipper;

import com.logistics.entity.ShippingRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.shipper.ShippingRequestShipperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shipper/shipping-requests")
public class ShippingRequestShipperController {

    @Autowired
    private ShippingRequestShipperService service;

    @GetMapping
    public ResponseEntity<List<ShippingRequest>> list() {
        List<ShippingRequest> list = service.listForCurrentShipper();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Boolean>> accept(@PathVariable Integer id) {
        ApiResponse<Boolean> res = service.accept(id);
        return ResponseEntity.status(res.isSuccess() ? 200 : 400).body(res);
    }
}
