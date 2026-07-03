package com.logistics.service.manager;

import com.logistics.dto.manager.shipment.ManagerShipmentListDto;
import com.logistics.dto.manager.shipment.ManagerShipmentPerformanceDto;
import com.logistics.request.SearchRequest;
import com.logistics.request.manager.shipment.ManagerOrdersShipmentSearchRequest;
import com.logistics.request.manager.shipment.ManagerShipmentAddEditRequest;
import com.logistics.request.manager.shipment.ManagerShipmentSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.manager.GetOrdersByShipmentIdManagerResponse;

import java.util.List;

public interface ShipmentManagerService {
    ListResponse<ManagerShipmentListDto> list(int userId, ManagerShipmentSearchRequest request);
    GetOrdersByShipmentIdManagerResponse getOrdersByShipmentId(int userId, int shipmentId, ManagerOrdersShipmentSearchRequest request);
    List<Integer> getAllOrderIdsByShipmentId(int userId, int shipmentId, ManagerOrdersShipmentSearchRequest request);
    void cancelShipment(Integer userId, Integer shipmentId);
    ListResponse<ManagerShipmentListDto> getPendingAndInTransitShipments(Integer userId, SearchRequest request);
    void create(int userId, ManagerShipmentAddEditRequest request);
    void update(int userId, Integer shipmentId, ManagerShipmentAddEditRequest request);
    ListResponse<ManagerShipmentPerformanceDto> getShipmentsByEmployeeId(int userId, int employeeId, SearchRequest request);
    byte[] exportShipmentsByEmployeeId(int userId, int employeeId, SearchRequest request);
    byte[] exportShipmentPerformance(Integer userId, Integer employeeId, SearchRequest request);
    List<ManagerShipmentPerformanceDto> getShipmentsByEmployeeIdForExport(
            int userId,
            int employeeId,
            SearchRequest request);
    byte[] export(int userId, ManagerShipmentSearchRequest request);
    byte[] exportOrdersByShipmentId(int userId, int shipmentId, ManagerOrdersShipmentSearchRequest request);
}