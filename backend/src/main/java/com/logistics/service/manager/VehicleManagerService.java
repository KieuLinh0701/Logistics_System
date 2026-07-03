package com.logistics.service.manager;

import com.logistics.dto.VehicleDto;
import com.logistics.request.manager.vehicle.ManagerVehicleEditRequest;
import com.logistics.request.manager.vehicle.ManagerVehicleSearchRequest;
import com.logistics.response.ListResponse;

import java.util.List;

public interface VehicleManagerService {
    ListResponse<VehicleDto> list(int userId, ManagerVehicleSearchRequest request);
    byte[] export(int userId, ManagerVehicleSearchRequest request);
    void update(int userId, int vehicleId, ManagerVehicleEditRequest request);
    List<VehicleDto> getAvailableVehicles(int userId);
}