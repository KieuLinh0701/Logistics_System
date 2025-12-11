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
public class UserPromotionDto {
    private Integer id;
    private String code;
    private String title;
    private String discountType;
    private BigDecimal discountValue;
    private Integer maxDiscountAmount;
    private LocalDateTime endDate;
}
