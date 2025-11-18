package com.logistics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShippingRateDto {
    private int id;
    private String regionType;
    private BigDecimal weightFrom;
    private BigDecimal weightTo;
    private BigDecimal price;
    private BigDecimal unit;
    private BigDecimal extraPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
