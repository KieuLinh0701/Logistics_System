package com.logistics.dto.user.settlement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSettlementSummaryResponse {
    private BigDecimal received;
    private BigDecimal pending;
    private BigDecimal debt;
}
