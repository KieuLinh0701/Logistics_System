package com.logistics.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminFinancialPoint {
    private LocalDate date;
    private BigDecimal systemAmount;
    private BigDecimal actualAmount;
}
