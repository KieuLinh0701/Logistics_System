package com.logistics.service.admin;

import com.logistics.request.admin.CreateVehicleRequest;
import com.logistics.request.admin.UpdateVehicleRequest;

import java.util.Map;

public interface VehicleAdminService {

    Map<String, Object> listVehicles(int page, int limit, String search, String type, String status);

    void createVehicle(CreateVehicleRequest request);

    void updateVehicle(Integer vehicleId, UpdateVehicleRequest request);

    void deleteVehicle(Integer vehicleId);

    Map<String, Object> getVehicleById(Integer vehicleId);

    Map<String, Object> getVehicleTrackings(Integer vehicleId);
}
