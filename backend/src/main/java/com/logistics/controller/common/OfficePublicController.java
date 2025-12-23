package com.logistics.controller.common;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.common.PublicOfficeInformationDto;
import com.logistics.dto.common.PublicOfficeSearchDto;
import com.logistics.request.common.office.PublicOfficeSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.common.OfficePublicService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/public/offices")
public class OfficePublicController {

    @Autowired
    private OfficePublicService service;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PublicOfficeSearchDto>>> searchOffices(@Valid PublicOfficeSearchRequest request) {
        ApiResponse<List<PublicOfficeSearchDto>> result = service.searchOffices(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/head-office")
    public ResponseEntity<ApiResponse<PublicOfficeInformationDto>> getHeadOffice() {
        ApiResponse<PublicOfficeInformationDto> result = service.getHeadOffice();
        return ResponseEntity.ok(result); 
    }

    @GetMapping("/region")
    public ResponseEntity<ApiResponse<List<PublicOfficeInformationDto>>> listLocalOffices(@Valid PublicOfficeSearchRequest officeSearchRequest,
    HttpServletRequest request) {
        ApiResponse<List<PublicOfficeInformationDto>> result = service.listLocalOffices(officeSearchRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/region/{cityCode}/check")
    public ResponseEntity<ApiResponse<Boolean>> checkLocalOffices(@PathVariable int cityCode,
    HttpServletRequest request) {
        ApiResponse<Boolean> result = service.checkLocalOffices(cityCode);
        return ResponseEntity.ok(result);
    }
}