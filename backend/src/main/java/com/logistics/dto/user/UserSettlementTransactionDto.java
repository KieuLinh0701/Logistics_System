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
public class UserSettlementTransactionDto {
    private Integer id;
    private String code;
    private BigDecimal amount;
    private String type;
    private String status;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private LocalDateTime paidAt;
}
