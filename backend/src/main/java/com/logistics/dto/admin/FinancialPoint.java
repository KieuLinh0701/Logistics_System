package com.logistics.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @deprecated Use {@link com.logistics.dto.admin.AdminFinancialPoint} instead.
 */
@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialPoint {
    private LocalDate date;
    private BigDecimal systemAmount;
    private BigDecimal actualAmount;
}
