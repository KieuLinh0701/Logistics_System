package com.logistics.service.manager;

import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.dto.manager.order.ManagerOrderListDto;
import com.logistics.dto.manager.order.ManagerOrderStatusCountResponse;
import com.logistics.entity.Office;
import com.logistics.entity.Order;
import com.logistics.request.manager.order.ManagerOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.request.user.order.UserUrgentOrderSearchRequest;
import com.logistics.response.BulkResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.manager.order.UrgentOrderResponse;

import java.util.List;

public interface OrderManagerService {
    ListResponse<ManagerOrderListDto> list(int userId, UserOrderSearchRequest request);
    List<ManagerOrderStatusCountResponse> getStatusCounts(Integer userId);
    List<Integer> getAllOrderIds(int userId, UserOrderSearchRequest request);
    ManagerOrderDetailDto getOrderByTrackingNumber(int userId, String trackingNumber);
    List<OrderPrintDto> getOrdersForPrint(Integer userId, List<Integer> orderIds);
    void cancelOrder(Integer userId, Integer orderId);
    void confirmOrder(Integer userId, Integer orderId);
    String create(Integer userId, ManagerOrderCreateRequest request);
    void update(Integer userId, Integer orderId, ManagerOrderCreateRequest request);
    byte[] export(Integer userId, UserOrderSearchRequest request);
    BulkResponse<String> confirmUrgentOrders(Integer userId, List<Integer> orderIds);
    boolean setOrderAtOriginOffice(Integer userId, Integer orderId);
    void confirmDestinationOrder(
            Integer userId,
            Integer orderId,
            boolean confirmed);
    BulkResponse<String> confirmDestinationOrders(
            Integer userId, List<Integer> orderIds, boolean confirmed);
    void setOrderReturned(Integer userId, Integer orderId);
    ListResponse<UrgentOrderResponse> getUrgentOrders(
            Integer userId,
            UserUrgentOrderSearchRequest request);
    byte[] exportUrgent(Integer userId, UserUrgentOrderSearchRequest request);
    void confirmUrgentOrder(Integer userId, Integer orderId);
    BulkResponse<String> confirmOrders(Integer userId, List<Integer> orderIds);
    BulkResponse<String> setOrdersAtOriginOffice(Integer userId, List<Integer> orderIds);
    BulkResponse<String> cancelOrders(Integer userId, List<Integer> orderIds);
    BulkResponse<String> setOrdersReturned(Integer userId, List<Integer> orderIds);
    List<Integer> getUrgentOrderIds(
            Integer userId,
            UserUrgentOrderSearchRequest request);
    boolean hasTransitPermission(Order order, Office office);
    
    void confirmReturnArrival(Integer userId, Integer orderId);
    BulkResponse<String> confirmReturnArrivals(Integer userId, List<Integer> orderIds);
}