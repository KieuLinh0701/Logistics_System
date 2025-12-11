package com.logistics.dto.user.order;

import java.math.BigDecimal;

import com.logistics.dto.AddressDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderListDto {
    private Integer id;
    private String trackingNumber;
    private String status;
    private AddressDto recipientAddress;
    private String pickupType;
    private BigDecimal weight;
    private String serviceTypeName;
    private Integer cod;
    private Integer orderValue;
    private Integer totalFee;
    private String payer;
    private String paymentStatus;;
}
