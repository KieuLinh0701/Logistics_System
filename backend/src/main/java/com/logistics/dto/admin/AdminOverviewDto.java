package com.logistics.dto.admin;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
