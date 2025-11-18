package com.logistics.dto.admin;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class UpdateOfficeRequest {
    private String code;
    private String postalCode;
    private String name;
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
    private Integer wardCode;
    private Integer cityCode;
    private String detailAddress;
}



