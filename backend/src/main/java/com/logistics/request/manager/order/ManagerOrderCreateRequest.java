package com.logistics.request.manager.order;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerOrderCreateRequest {
    private String senderName;
    private String senderDetail;
    private Integer senderWardCode;
    private Integer senderCityCode;
    private String senderPhone;
    private String recipientName;
    private String recipientPhone;
    private Integer recipientCityCode;
    private Integer recipientWardCode;
    private String recipientDetail;
    private BigDecimal weight;
    private Integer serviceTypeId;
    private Integer orderValue;
    private String payer;
    private String notes;
}