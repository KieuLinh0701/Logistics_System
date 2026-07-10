package com.logistics.service.shipper;

import com.logistics.entity.Employee;
import com.logistics.entity.Order;
import com.logistics.enums.OrderStatus;

public interface ShipperVehicleWorkloadService {

    void addLoaded(Order order, Employee employee);

    void removeLoaded(Order order, Employee employee);

    void applyTransition(Order order, Employee employee, OrderStatus oldStatus, OrderStatus newStatus);
}