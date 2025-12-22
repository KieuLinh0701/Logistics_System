package com.logistics.controller.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.dto.manager.incidentReport.ManagerIncidentReportDetailDto;
import com.logistics.dto.manager.incidentReport.ManagerIncidentReportListDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestDetailDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestListDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.incidentReport.ManagerIncidentUpdateRequest;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestForm;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.IncidentReportManagerService;
import com.logistics.service.manager.ShippingRequestManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/manager/incident-reports")
public class IncidentReportManagerController {

    @Autowired
    private IncidentReportManagerService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<ManagerIncidentReportListDto>>> list(
            @Valid SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<ListResponse<ManagerIncidentReportListDto>> result = service.list(userId,
                searchRequest);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagerIncidentReportDetailDto>> getById(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ApiResponse<ManagerIncidentReportDetailDto> result = service.getById(userId, id);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> processing(
            @PathVariable Integer id,
            @RequestBody ManagerIncidentUpdateRequest updateRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(service.processing(userId, id, updateRequest));
    }
}