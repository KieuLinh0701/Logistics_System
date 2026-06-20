package com.logistics.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @deprecated Use {@link com.logistics.dto.admin.AdminShopReportDto} instead.
 */
@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopReportDto {
    private Integer shopId;
    private String shopName;
    private Long ordersCount;
    private BigDecimal totalOrderValue;
    private BigDecimal totalShippingFee;
}
