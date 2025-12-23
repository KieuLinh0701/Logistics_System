package com.logistics.dto.manager.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.logistics.dto.AddressDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerOrderListDto {
    private Integer id;
    private String trackingNumber;
    private String status;
    private String senderName;
    private String senderPhone;
    private Integer senderCityCode;
    private Integer senderWardCode;
    private String senderDetail;
    private AddressDto recipientAddress;
    private String pickupType;
    private BigDecimal weight;
    private String serviceTypeName;
    private Integer cod;
    private Integer orderValue;
    private Integer totalFee;
    private String payer;
    private String paymentStatus;
    private String createdByType;
    private String employeeCode;
    private String userCode;
    private LocalDateTime createdAt;
    private LocalDateTime deliveriedAt;
    private LocalDateTime paidAt;
    private String codStatus;
}
