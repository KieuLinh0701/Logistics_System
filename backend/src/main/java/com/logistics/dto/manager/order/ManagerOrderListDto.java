package com.logistics.dto.manager.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String senderFullAddress;
    private String recipientName;
    private String recipientPhone;
    private String recipientFullAddress;
    private String pickupType;
    private BigDecimal weight;
    private BigDecimal adjustedWeight;
    private String serviceTypeName;
    private Integer cod;
    private Integer orderValue;
    private Integer totalFee;
    private BigDecimal actualCollected;
    private BigDecimal returnedAmount;
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
