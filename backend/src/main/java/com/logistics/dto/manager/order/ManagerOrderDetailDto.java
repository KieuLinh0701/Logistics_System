package com.logistics.dto.manager.order;

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
public class ManagerOrderDetailDto {
    private Integer id;
    private String trackingNumber;
    private String status;
    private String createdByType;
    private String senderName;
    private String senderPhone;
    private Integer senderCityCode;
    private Integer senderWardCode;
    private String senderDetail;
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
    private OfficeDto fromOffice;
    private OfficeDto toOffice;
    private List<OrderProductDto> orderProducts;
    private List<OrderHistoryDto> orderHistories;
    private String employeeCode;
    private String userCode;
}