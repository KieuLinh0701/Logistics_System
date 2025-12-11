package com.logistics.request.user.order;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderCreateRequest {
    private String status;
    private Integer senderAddressId;
    private String recipientName;
    private String recipientPhone;
    private Integer recipientCityCode;
    private Integer recipientWardCode;
    private String recipientDetail;
    private String pickupType;
    private BigDecimal weight;
    private Integer serviceTypeId;
    private Integer cod;
    private Integer orderValue;
    private String payer;
    private String notes;

    private Integer fromOfficeId;
    private Integer promotionId;
    private List<OrderProduct> orderProducts;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderProduct {
        private Integer productId;
        private Integer quantity;
    }
}