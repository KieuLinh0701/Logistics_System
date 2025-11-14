package com.logistics.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.OfficeDto;
import com.logistics.request.office.OfficeSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.OfficeService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/public/offices")
public class PublicOfficeController {

    @Autowired
    private OfficeService service;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OfficeDto>>> searchOffices(@Valid OfficeSearchRequest request) {
        ApiResponse<List<OfficeDto>> result = service.searchOffices(request);
        return ResponseEntity.ok(result);
    }
}