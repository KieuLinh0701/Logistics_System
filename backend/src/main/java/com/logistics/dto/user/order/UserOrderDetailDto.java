package com.logistics.dto.user.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.logistics.dto.AddressDto;
import com.logistics.dto.OfficeDto;
import com.logistics.dto.OrderHistoryDto;
import com.logistics.dto.OrderProductDto;
import com.logistics.dto.ServiceTypeDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderDetailDto {
    private Integer id;
    private String trackingNumber;
    private String status;
    private String createdByType;
    private String senderName;
    private String senderPhone;
    private Integer senderCityCode;
    private String senderCityName;
    private Integer senderWardCode;
    private String senderWardName;
    private String senderFullAddress;
    private Double senderLatitude;
    private Double senderLongitude;
    private String senderDetail;
    private AddressDto senderAddress;
    private AddressDto recipientAddress;
    private String recipientName;
    private String recipientPhone;
    private Integer recipientCityCode;
    private String recipientCityName;
    private Integer recipientWardCode;
    private String recipientWardName;
    private String recipientFullAddress;
    private Double recipientLatitude;
    private Double recipientLongitude;
    private String recipientDetail;

    private BigDecimal weight;
    private String pickupType;
    private BigDecimal originalWeight;
    private BigDecimal height;
    private BigDecimal width;
    private BigDecimal length;
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
    private List<OrderProductDto> orderProducts;
    private List<OrderHistoryDto> orderHistories;
    private Promotion promotion;
    private String codStatus;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Promotion {
        private Integer id;
        private String code;
    }
}
