package com.logistics.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.ServiceTypeDto;
import com.logistics.enums.ServiceTypeStatus;
import com.logistics.response.ApiResponse;
import com.logistics.service.ServiceTypeService;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/public/service-types")
public class PublicServiceTypeController {

    @Autowired
    private ServiceTypeService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ServiceTypeDto>>> getActiveServiceTypes() {
        ApiResponse<List<ServiceTypeDto>> result = service.getServicesByStatus(ServiceTypeStatus.ACTIVE);
        return ResponseEntity.ok(result);
    }
}