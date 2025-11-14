package com.logistics.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {
    private Integer id;
    private String wardCode;
    private String cityCode;
    private String detail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
