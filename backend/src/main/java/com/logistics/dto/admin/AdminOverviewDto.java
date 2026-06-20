package com.logistics.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOverviewDto {
    private Long totalOffices;
    private Long totalEmployees;
    private Long totalShippers;

    private Long totalOrders;
    private Long delivered;
    private Long failed;
    private Long returnedOrders;
    private Long inProgress;
    private double successRate; // percent

    private BigDecimal shippingRevenue;

    private BigDecimal totalCodCollected;
    private BigDecimal codTransferred;
    private BigDecimal codHeld;
}
