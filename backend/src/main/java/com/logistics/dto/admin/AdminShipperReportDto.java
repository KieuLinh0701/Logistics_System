package com.logistics.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminShipperReportDto {
    private Integer shipperId;
    private String fullName;
    private String phoneNumber;
    private Long ordersCount;
    private BigDecimal systemCod;
    private BigDecimal actualCod;
    private BigDecimal diff;
}
