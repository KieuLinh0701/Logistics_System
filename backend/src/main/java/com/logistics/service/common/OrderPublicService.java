package com.logistics.service.common;

import com.logistics.dto.OrderHistoryDto;

import java.util.List;

public interface OrderPublicService {
    List<OrderHistoryDto> getOrderHistoriesByTrackingNumber(String trackingNumber);
}