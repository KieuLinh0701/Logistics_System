package com.logistics.controller.common;

import com.logistics.dto.ServiceTypeDto;
import com.logistics.dto.ServiceTypeWithRateDto;
import com.logistics.enums.ServiceTypeStatus;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.ServiceTypePublicService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/service-types")
@Tag(name = "Public - Service Type", description = "Tra cứu thông tin các loại dịch vụ vận chuyển và bảng giá")
public class ServiceTypePublicController {

    @Autowired
    private ServiceTypePublicService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ServiceTypeDto>>> getActiveServiceTypes() {
        List<ServiceTypeDto> result = service.getServicesByStatus(ServiceTypeStatus.ACTIVE);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/active-with-rates")
    public ResponseEntity<ApiResponse<List<ServiceTypeWithRateDto>>> getActiveServicesWithRates() {
        List<ServiceTypeWithRateDto> result = service.getActiveServicesWithRates();
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}