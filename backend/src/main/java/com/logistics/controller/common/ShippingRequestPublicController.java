package com.logistics.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.request.common.shippingRequest.PublicShippingRequestForm;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.ShippingRequestPublicService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/public/shipping-requests")
public class ShippingRequestPublicController {

    @Autowired
    private ShippingRequestPublicService service;

    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(
        @Valid @RequestBody PublicShippingRequestForm requestForm) {
        
        return ResponseEntity.ok(service.create(requestForm));
    }
}