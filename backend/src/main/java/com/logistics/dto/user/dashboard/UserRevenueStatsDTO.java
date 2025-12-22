package com.logistics.dto.user.dashboard;

import java.math.BigDecimal;

import lombok.*;

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
