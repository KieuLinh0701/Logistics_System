package com.logistics.dto.common;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PublicOfficeInformationDto {
    private Integer id;
    private String code;
    private String name;
    private Integer cityCode;
    private Integer wardCode;
    private String detail;
    private String email;
    private String phoneNumber;
    private LocalTime openingTime;
    private LocalTime closingTime;
}
