package com.logistics.service.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.response.BulkResponse;

import java.util.List;

public interface ShipmentOrderManagerService {
    BulkResponse<ManagerShipmentDetailDto> checkOrdersForShipment(
            Integer userId, Integer shipmentId, List<String> trackingNumbers);

    BulkResponse<String> saveShipmentOrders(
            Integer userId, Integer shipmentId, List<Integer> removedOrderIds, List<Integer> addedOrderIds);
}