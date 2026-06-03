package com.logistics.dto.manager.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManagerAiUnassignedOrderDto {
    private Integer orderId;
    private String trackingNumber;
    private String reason;
}
