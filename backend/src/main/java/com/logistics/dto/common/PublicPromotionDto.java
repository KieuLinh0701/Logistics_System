package com.logistics.dto.common;

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
public class PublicPromotionDto {
    private Integer id;
    private String code;
    private String title;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private Integer maxDiscountAmount;
    private BigDecimal minOrderValue;
    private Integer usedCount;
    private Integer usageLimit;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
