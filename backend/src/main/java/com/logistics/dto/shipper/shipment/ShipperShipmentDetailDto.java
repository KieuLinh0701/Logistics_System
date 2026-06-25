package com.logistics.dto.shipper.shipment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShipperShipmentDetailDto {
    private Integer id;
    private String trackingNumber;
    private String status;
    private BigDecimal weight;
    private Integer cod;
    private Integer totalFee;
    private String payer;
    private String paymentStatus;
    private String recipientName;
    private String recipientPhone;
    private String recipientFullAddress;
    private String senderName;
    private String senderPhone;
    private String senderFullAddress;
}