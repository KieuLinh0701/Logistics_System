package com.logistics.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}