package com.logistics.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceTypeWithRateDto {
    private int id;
    private String name;
    private String deliveryTime;
    private List<ShippingRateDto> rates;
}
