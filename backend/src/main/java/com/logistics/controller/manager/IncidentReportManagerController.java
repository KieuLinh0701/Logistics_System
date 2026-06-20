package com.logistics.controller.manager;

import com.logistics.dto.manager.incidentReport.ManagerIncidentReportDetailDto;
import com.logistics.dto.manager.incidentReport.ManagerIncidentReportListDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.incidentReport.ManagerIncidentUpdateRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.IncidentReportManagerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/manager/incident-reports")
@Tag(name = "Manager - Incident Report", description = "Quản lý và xử lý các báo cáo sự cố vận hành tại bưu cục")
public class IncidentReportManagerController {

    @Autowired
    private IncidentReportManagerService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<ListResponse<ManagerIncidentReportListDto>>> list(
            @Valid SearchRequest searchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ListResponse<ManagerIncidentReportListDto> result = service.list(userId,
                searchRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagerIncidentReportDetailDto>> getById(
            @PathVariable Integer id,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        ManagerIncidentReportDetailDto result = service.getById(userId, id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> processing(
            @PathVariable Integer id,
            @RequestBody ManagerIncidentUpdateRequest updateRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        service.processing(userId, id, updateRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            HttpServletRequest request,
            SearchRequest searchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, searchRequest);

        String fileName = "UTE Logistics_Báo cáo sự cố bưu cục.xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }
}