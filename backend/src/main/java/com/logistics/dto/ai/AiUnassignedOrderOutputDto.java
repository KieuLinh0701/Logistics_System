package com.logistics.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AiUnassignedOrderOutputDto {
    @JsonProperty("order_id")
    @JsonAlias({"orderId"})
    private Integer orderId;

    @JsonProperty("tracking_number")
    @JsonAlias({"trackingNumber"})
    private String trackingNumber;

    private String reason;
}
