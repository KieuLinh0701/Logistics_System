package com.logistics.controller.manager;

import com.logistics.dto.VehicleDto;
import com.logistics.request.manager.vehicle.ManagerVehicleEditRequest;
import com.logistics.request.manager.vehicle.ManagerVehicleSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.service.manager.VehicleManagerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

        return ResponseEntity.ok(service.list(userId, managerVehicleSearchRequest));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Integer id,
            @Valid @RequestBody ManagerVehicleEditRequest managerVehicleEditRequest,
            HttpServletRequest request) {

        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(service.update(userId, id, managerVehicleEditRequest));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VehicleDto>>> getAvailableVehicles(
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("currentUserId");
        return ResponseEntity.ok(service.getAvailableVehicles(userId));
    }
}
