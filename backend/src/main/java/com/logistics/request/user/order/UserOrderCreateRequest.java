package com.logistics.request.user.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

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
    private String recipientWardName;
    private String recipientCityName;
    private Double recipientLatitude;
    private Double recipientLongitude;
    private Integer recipientAddressId;
    private Boolean saveRecipient;
    private String pickupType;
    private BigDecimal weight;
    private BigDecimal originalWeight;
    private BigDecimal height;
    private BigDecimal length;
    private BigDecimal width;
    private Integer serviceTypeId;
    private Integer cod;
    private Integer orderValue;
    private String payer;
    private String notes;

    private Integer fromOfficeId;
    private Integer promotionId;
    private List<OrderProduct> orderProducts;
    private Integer shippingFee;
    private Integer discountAmount;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderProduct {
        private Integer productId;
        private Integer quantity;
    }
}