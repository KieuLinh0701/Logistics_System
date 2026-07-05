package com.logistics.service.common;

import com.logistics.entity.Office;
import com.logistics.entity.Order;

public interface OrderOriginService {
    boolean isOriginOffice(Order order, Office office);
}