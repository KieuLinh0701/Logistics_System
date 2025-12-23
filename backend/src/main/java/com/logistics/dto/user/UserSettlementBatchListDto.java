package com.logistics.dto.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserSettlementBatchListDto {
    private Integer id;
    private String code;
    private BigDecimal balanceAmount;
    private BigDecimal remainAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 
