package com.logistics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

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
