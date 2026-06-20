package com.logistics.dto;

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
public class ProductDto {
    private Integer id;
    private String code;
    private String image;
    private String name;
    private BigDecimal weight;
    private Integer price;
    private String type;
    private String status;
    private Integer stock;
    private Integer soldQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
