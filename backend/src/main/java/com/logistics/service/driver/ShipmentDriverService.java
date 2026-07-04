package com.logistics.service.driver;

import com.logistics.request.driver.FinishShipmentRequest;
import com.logistics.request.driver.UpdateVehicleTrackingRequest;

import java.util.Map;

public interface ShipmentDriverService {

    void startShipment(Integer shipmentId);

    void finishShipment(FinishShipmentRequest request);

    Map<String, Object> getShipments(int page, int limit);

    Map<String, Object> getRoute();

    Map<String, Object> getHistory(int page, int limit);

    void updateVehicleTracking(UpdateVehicleTrackingRequest request);

    Map<String, Object> getVehicleTracking(Integer shipmentId);
}
