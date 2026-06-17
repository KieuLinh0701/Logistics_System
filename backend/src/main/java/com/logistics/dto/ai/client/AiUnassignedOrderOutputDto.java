package com.logistics.dto.ai.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiUnassignedOrderOutputDto {
    private Integer orderId;
    private String trackingNumber;
    private String reason;
}
