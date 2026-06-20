package com.logistics.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @deprecated Use {@link com.logistics.dto.admin.AdminShipperReportDto} instead.
 */
@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipperReportDto {
    private Integer shipperId;
    private String fullName;
    private String phoneNumber;
    private Long ordersCount;
    private BigDecimal systemCod;
    private BigDecimal actualCod;
    private BigDecimal diff;
}
