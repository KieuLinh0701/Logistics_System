package com.logistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductDto {
    private Integer productId;
    private Integer productPrice;
    private String productType;
    private Integer productStock;
    private String productCode;
    private String productName;
    private BigDecimal productWeight;
    private Integer quantity;
    private Integer price;
    private Integer deliveredQuantity;
    private Integer returnedQuantity;
}