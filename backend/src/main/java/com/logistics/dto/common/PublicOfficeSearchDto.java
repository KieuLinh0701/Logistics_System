package com.logistics.dto.common;

import java.math.BigDecimal;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PublicOfficeSearchDto {
    private Integer id;
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
}
