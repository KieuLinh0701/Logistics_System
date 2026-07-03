package com.logistics.controller.common;

import com.logistics.request.common.shippingRequest.PublicShippingRequestForm;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.ShippingRequestPublicService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/shipping-requests")
@Tag(name = "Public - Shipping Request", description = "Gửi yêu cầu vận chuyển công khai")
public class ShippingRequestPublicController {

    private final ShippingRequestPublicService service;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
        @Valid @RequestBody PublicShippingRequestForm requestForm) {

        service.create(requestForm);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}