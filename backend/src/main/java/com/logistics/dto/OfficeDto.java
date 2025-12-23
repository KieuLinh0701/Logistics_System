package com.logistics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OfficeDto {
    private Integer id;
    private String code;
    private String postalCode;
    private String name;
    private Integer cityCode;
    private Integer wardCode;
    private String detail;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String email;
    private String phoneNumber;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String type;
    private String status;
    private Integer capacity;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
