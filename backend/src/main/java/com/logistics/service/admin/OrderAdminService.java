package com.logistics.service.admin;

import com.logistics.dto.manager.order.ManagerOrderDetailDto;

import java.util.Map;

public interface OrderAdminService {

    Map<String, Object> listOrders(int page, int limit, String search, String status);

    ManagerOrderDetailDto getOrderById(Integer orderId);

    void deleteOrder(Integer orderId);
}
