package com.logistics.service.common;

import com.logistics.dto.order.OrderFulfillmentSummaryDto;

public interface OrderFulfillmentService {
    OrderFulfillmentSummaryDto getFulfillmentSummary(Long orderId);
}