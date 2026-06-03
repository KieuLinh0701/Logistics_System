package com.logistics.dto.ai.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiRouteStopOutputDto {
    private Integer orderId;
    private String trackingNumber;
    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private Double latitude;
    private Double longitude;
    private Integer codAmount;
    private String priority;
    private Integer stopSequence;
    private String etaTime;
    private Integer etaMinutesFromStart;
    private Double legDistanceKm;
}
