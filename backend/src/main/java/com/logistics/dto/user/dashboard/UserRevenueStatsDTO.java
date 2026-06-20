package com.logistics.dto.user.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRevenueStatsDTO {
    private BigDecimal received;
    private BigDecimal nextSettlement;
    private BigDecimal pendingDebt;
    private String nextSettlementDate;
}
