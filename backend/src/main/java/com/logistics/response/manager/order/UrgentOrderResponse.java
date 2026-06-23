package com.logistics.response.manager.order;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UrgentOrderResponse {
    private Integer id;
    private String trackingNumber;
    private String senderFullAddress;
    private String senderWardName;
    private String senderCityName;
    private Integer senderWardCode;
    private Integer senderCityCode;
    private LocalDateTime readyForPickupAt;
}
