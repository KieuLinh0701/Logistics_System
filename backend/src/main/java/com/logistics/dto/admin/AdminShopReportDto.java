package com.logistics.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminShopReportDto {
    private Integer shopId;
    private String shopName;
    private Long ordersCount;
    private BigDecimal totalOrderValue;
    private BigDecimal totalShippingFee;
}
