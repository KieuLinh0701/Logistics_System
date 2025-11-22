package com.logistics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    private Integer id;
    private String trackingNumber;
    private String status;
    private String createdByType;
    private String senderName;
    private String senderPhone;
    private AddressDto senderAddress;
    private String recipientName;
    private String recipientPhone;
    private AddressDto recipientAddress;
    private String pickupType;
    private BigDecimal weight;
    private ServiceTypeDto serviceType;
    private Integer discountAmount;
    private Integer cod;
    private Integer orderValue;
    private Integer totalFee;
    private String payer;
    private String paymentStatus;
    private String notes;
    private LocalDateTime deliveredAt;
    private LocalDateTime paidAt;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;
}
