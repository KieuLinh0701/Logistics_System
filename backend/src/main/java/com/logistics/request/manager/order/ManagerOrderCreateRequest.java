package com.logistics.request.manager.order;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerOrderCreateRequest {
    private String senderName;
    private String senderPhone;
    private String senderDetail;
    private Integer senderWardCode;
    private String senderWardName;
    private Integer senderCityCode;
    private String senderCityName;
    private Double senderLatitude;
    private Double senderLongitude;
    private String recipientName;
    private String recipientPhone;
    private String recipientDetail;
    private Integer recipientWardCode;
    private String recipientWardName;
    private Integer recipientCityCode;
    private String recipientCityName;
    private Double recipientLatitude;
    private Double recipientLongitude;
    private BigDecimal weight;
    private BigDecimal originalWeight;
    private BigDecimal height;
    private BigDecimal length;
    private BigDecimal width;
    private Integer serviceTypeId;
    private Integer orderValue;
    private String payer;
    private String notes;
    private String pickupType;
    private Integer fromOfficeId;
}