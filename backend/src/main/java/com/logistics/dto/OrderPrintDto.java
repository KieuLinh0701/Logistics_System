package com.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderPrintDto {
    private String trackingNumber;
    private String barcodeTrackingNumber;
    private String fromOfficeCode;
    private String qrFromOfficeCode;
    private AddressSummaryDto senderAddress;
    private AddressSummaryDto recipientAddress;
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
