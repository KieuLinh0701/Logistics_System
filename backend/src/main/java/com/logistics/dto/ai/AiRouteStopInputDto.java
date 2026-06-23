package com.logistics.dto.ai;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiRouteStopInputDto {
    private Long stopId;
    private Integer orderId;
    private String trackingNumber;
    private String stopType; // DELIVERY, PICKUP
    private String recipientName;
    private String recipientPhone;
    private String address;
    private Integer wardCode;
    private Integer cityCode;
    private Double latitude;
    private Double longitude;
    @Builder.Default
    private Integer codAmount = 0;
    private String priority;
    private Integer serviceTimeMinutes;
    private Double weightKg;
}
