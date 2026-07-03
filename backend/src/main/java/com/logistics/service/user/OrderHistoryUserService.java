package com.logistics.service.user;

import com.logistics.entity.Office;
import com.logistics.entity.Order;
import com.logistics.entity.Shipment;
import com.logistics.enums.OrderHistoryActionType;

public interface OrderHistoryUserService {
    void save(Order order, Office fromOffice, Office toOffice, Shipment shipment, OrderHistoryActionType action, String note);
}