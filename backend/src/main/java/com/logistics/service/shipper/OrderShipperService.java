package com.logistics.service.shipper;

import com.logistics.entity.Employee;
import com.logistics.entity.Order;
import com.logistics.enums.OrderStatus;
import com.logistics.request.shipper.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface OrderShipperService {

    String uploadProofImage(MultipartFile file);

    Order quickClaimOrderForPickup(Integer orderId, Employee employee);

    void applyVehicleWorkloadByStatus(Order order, Employee employee, OrderStatus newStatus);

    void applyVehicleWorkloadByStatus(Order order, Employee employee, OrderStatus oldStatus, OrderStatus newStatus);

    Map<String, Object> getDashboard();

    Map<String, Object> listOrders(int page, int limit, String status, String search);

    Map<String, Object> listUnassignedOrders(int page, int limit, Double latitude, Double longitude);

    Map<String, Object> listReturnOrders(int page, int limit, String status, String search);

    Map<String, Object> listPickupByCourierRequests(int page, int limit);

    Map<String, Object> listPickedUpByCustomerOrders(int page, int limit, String search);

    Map<String, Object> getOrderById(Integer id);

    Map<String, Object> getOrderByTrackingNumber(String trackingNumber);

    Map<String, Object> buildOrderDetail(Order order);

    void acceptPickupRequest(Integer id);

    void startPickup(Integer id);

    void claimOrder(Integer id);

    void unclaimOrder(Integer id);

    void recordDeliveryAttempt(Integer id, UpdateDeliveryStatusRequest request);

    void updateDeliveryStatus(Integer id, UpdateDeliveryStatusRequest request);

    boolean markPickedUp(Integer id, PickedUpRequest request);

    boolean markPickedUpByTrackingNumber(String trackingNumber, PickedUpRequest request);

    void retryPickup(Integer id);

    void deliverToOrigin(Integer id, DeliverOriginRequest request);

    void confirmDestinationOffice(Integer orderId);

    Map<String, Object> getPendingDestinationConfirmOrders();

    Map<String, Object> getDeliveryHistory(int page, int limit, String status);

    Map<String, Object> createIncidentReport(CreateIncidentReportRequest request);

    Map<String, Object> createIncidentReport(Integer orderId, String incidentType, String title, String description, String priority, MultipartFile[] images);

    List<Map<String, Object>> listIncidentReports();

    Map<String, Object> getIncidentDetail(Integer id);

    void returnFailedToOffice(Integer id);

    Map<String, Object> getDeliveryRoute();

    void startRoute(Integer routeId);

    Map<String, Object> reOptimizeShipmentRoute(ShipperReOptimizeRequest request);

    Map<String, Object> reOptimizeRoute(ShipperReOptimizeRequest request);

    Map<String, Object> assignPickupToShipperRoute(PickupInsertionRequest request);

    Map<String, Object> insertPickupIntoShipment(Integer shipmentId, InsertPickupShipmentRequest request);
}
