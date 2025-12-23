package com.logistics.dto.user.dashboard;

import java.util.Map;

import com.logistics.enums.ProductType;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProductTypeCountDto {
    private Map<ProductType, Long> counts;
}