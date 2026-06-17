package com.logistics.dto.manager.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.logistics.dto.AddressDto;
import com.logistics.dto.OfficeDto;
import com.logistics.dto.OrderHistoryDto;
import com.logistics.dto.OrderProductDto;
import com.logistics.dto.PickupAttemptDto;
import com.logistics.dto.ServiceTypeDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerOrderDetailDto {
    private Integer id;
    private String trackingNumber;
    private String status;
    private String createdByType;

    private AddressDto senderAddress;
    private int senderWardCode;
    private int senderCityCode;
    private String senderDetail;
    private String senderName;
    private String senderPhone;
    private String senderFullAddress;
    private String senderCityName;
    private String senderWardName;
    private Double senderLatitude;
    private Double senderLongitude;

    private AddressDto recipientAddress;
    private int recipientWardCode;
    private int recipientCityCode;
    private String recipientDetail;
    private String recipientName;
    private String recipientPhone;
    private String recipientFullAddress;
    private String recipientCityName;
    private String recipientWardName;
    private Double recipientLatitude;
    private Double recipientLongitude;

    private String pickupType;
    private BigDecimal originalWeight;
    private BigDecimal height;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal weight;
    private BigDecimal adjustedWeight;
    private BigDecimal adjustedOriginalWeight;
    private BigDecimal adjustedHeight;
    private BigDecimal adjustedWidth;
    private BigDecimal adjustedLength;
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
    private OfficeDto fromOffice;
    private OfficeDto toOffice;
    private List<OrderProductDto> orderProducts;
    private List<OrderHistoryDto> orderHistories;
    private List<PickupAttemptDto> pickupAttempts;
    private String employeeCode;
    private String userCode;
    private String codStatus;
    private BigDecimal actualCollected;
    private BigDecimal returnedAmount;
}