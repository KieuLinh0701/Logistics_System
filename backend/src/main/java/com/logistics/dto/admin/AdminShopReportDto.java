package com.logistics.dto.admin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
