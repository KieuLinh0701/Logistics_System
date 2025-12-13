package com.logistics.dto.manager.shipment;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShipmentDetailDto {
    private Integer id;
    private String trackingNumber;
    private String status;

    private Recipient recipient;
    private Office toOffice;

    private BigDecimal weight;
    private Integer cod;
    private Integer totalFee;
    private String payer;
    private String paymentStatus;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Recipient {
        private String name;
        private String phone;
        private Integer cityCode;
        private Integer wardCode;
        private String detail;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Office {
        private String name;
        private String postalCode;
        private Integer cityCode;
        private Integer wardCode;
        private String detail;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }
}