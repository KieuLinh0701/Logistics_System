package com.logistics.service.user;

import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.user.order.UserOrderDetailDto;
import com.logistics.dto.user.order.UserOrderListDto;
import com.logistics.dto.user.order.UserOrderStatusCountResponse;
import com.logistics.entity.Order;
import com.logistics.request.user.order.UserOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.response.BulkResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.OrderCreateSuccess;

import java.math.BigDecimal;
import java.util.List;

public interface OrderUserService {
    ListResponse<UserOrderListDto> list(int userId, UserOrderSearchRequest request);
    List<UserOrderStatusCountResponse> getStatusCounts(Integer userId);
    List<Integer> getAllOrderIds(int userId, UserOrderSearchRequest request);
    OrderCreateSuccess create(Integer userId, UserOrderCreateRequest request);
    UserOrderDetailDto getOrderByTrackingNumber(int userId, String trackingNumber);
    UserOrderDetailDto getOrderById(int userId, int id);
    String publicOrder(Integer userId, Integer orderId);
    BulkResponse<String> publicOrders(Integer userId, List<Integer> orderIds);
    void cancelOrder(Integer userId, Integer orderId);
    BulkResponse<String> cancelOrders(Integer userId, List<Integer> orderIds);
    void updateOrder(Integer userId, Integer orderId, UserOrderCreateRequest request);
    List<OrderPrintDto> getOrdersForPrint(Integer userId, List<Integer> orderIds);
    void setOrderReadyForPickup(Integer userId, Integer orderId);
    BulkResponse<String> setOrdersReadyForPickup(Integer userId, List<Integer> orderIds);
    void setOrderTransitToOffice(Integer userId, Integer orderId);
    BulkResponse<String> setOrdersTransitToOffice(Integer userId, List<Integer> orderIds);
    void deleteOrder(Integer userId, Integer orderId);
    BulkResponse<String> deleteOrders(Integer userId, List<Integer> orderIds);
    byte[] export(Integer userId, UserOrderSearchRequest request);
    BigDecimal calculateWeight(List<UserOrderCreateRequest.OrderProduct> items,
                               BigDecimal originalWeight, BigDecimal height,
                               BigDecimal length, BigDecimal width);

    int calculateOrderValue(List<UserOrderCreateRequest.OrderProduct> items, Integer orderValue);

    void saveOrderProducts(Order order, List<UserOrderCreateRequest.OrderProduct> items);
}