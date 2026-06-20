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
public class ShippingRateDto {
    private int id;
    private String regionType;
    private BigDecimal weightFrom;
    private BigDecimal weightTo;
    private BigDecimal price;
    private BigDecimal unit;
    private BigDecimal extraPrice;
}
