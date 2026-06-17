package com.logistics.dto.manager.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManagerAiRouteStopDto {
    private Long stopId;
    private Integer orderId;
    private Integer stopSequence;
    private String trackingNumber;
    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private Double latitude;
    private Double longitude;
    private Integer codAmount;
    private String priority;
    private String etaTime;
    private Integer etaMinutesFromStart;
}
