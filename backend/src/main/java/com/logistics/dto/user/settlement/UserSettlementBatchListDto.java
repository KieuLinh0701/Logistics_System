package com.logistics.dto.user.settlement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSettlementBatchListDto {
    private Integer id;
    private String code;
    private BigDecimal balanceAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
