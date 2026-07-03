package com.logistics.service.common;

import com.logistics.entity.Office;
import com.logistics.entity.Order;

public interface OrderDestinationService {
    boolean isDestinationOffice(Order order, Office office);
}