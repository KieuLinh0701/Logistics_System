package com.logistics.dto.admin;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
