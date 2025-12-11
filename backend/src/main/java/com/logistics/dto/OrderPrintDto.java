package com.logistics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderPrintDto {
    private String trackingNumber;
    private String barcodeTrackingNumber;
    private String fromOfficeCode;
    private String qrFromOfficeCode;
    private String senderName;
    private String senderPhone;
    private String senderCityCode;
    private String senderWardCode;
    private String senderDetail;
    private AddressDto recipientAddress;
    private Integer codAmount;
    private BigDecimal weight;
    private LocalDateTime createdAt;
    private List<OrderProductDto> orderProducts;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderProductDto {
        private String productName;
        private Integer quantity;
    }

}
