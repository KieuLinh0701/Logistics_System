package com.logistics.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PublicOfficeInformationDto {
    private Integer id;
    private String postalCode;
    private String name;
    private Integer cityCode;
    private Integer wardCode;
    private String detail;
    private String email;
    private String phoneNumber;
    private LocalTime openingTime;
    private LocalTime closingTime;
}
