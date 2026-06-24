package com.logistics.dto.manager.shipment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShipmentDetailDto {
    private Integer id;
    private String trackingNumber;
    private String status;

    private Office toOffice;
    private Office currentOffice;

    private BigDecimal weight;
    private Integer cod;
    private Integer totalFee;
    private String payer;
    private String paymentStatus;
    private boolean pendingDestinationConfirm;
    private String recipientName;
    private String recipientPhone;
    private String recipientFullAddress;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Office {
        private Integer id;
        private String name;
        private String postalCode;
        private Integer cityCode;
        private Integer wardCode;
        private String detail;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }
}