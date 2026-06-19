package com.logistics.controller.manager;

import com.logistics.dto.VehicleDto;
import com.logistics.request.manager.employee.ManagerEmployeeSearchRequest;
import com.logistics.request.manager.vehicle.ManagerVehicleEditRequest;
import com.logistics.request.manager.vehicle.ManagerVehicleSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.VehicleManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/vehicles")
public class VehicleManagerController {

    @Autowired
    private VehicleManagerService service;

    @GetMapping
    public ResponseEntity<ApiResponse<ListResponse<VehicleDto>>> list(
            @Valid ManagerVehicleSearchRequest managerVehicleSearchRequest,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.list(userId, managerVehicleSearchRequest)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Integer id,
            @Valid @RequestBody ManagerVehicleEditRequest managerVehicleEditRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");

        service.update(userId, id, managerVehicleEditRequest);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VehicleDto>>> getAvailableVehicles(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");

        return ResponseEntity.ok(ApiResponse.success(service.getAvailableVehicles(userId)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(HttpServletRequest request,
                                         ManagerVehicleSearchRequest managerVehicleSearchRequest) throws Exception {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        byte[] data = service.export(userId, managerVehicleSearchRequest);

        String fileName = "UTE Logistics_Báo cáo danh sách phương tiện của bưu cục.xlsx";
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
