package com.logistics.mapper;

import com.logistics.dto.ShippingRateDto;
import com.logistics.entity.ShippingRate;

public class ShippingRateMapper {

    public static ShippingRateDto toDto(ShippingRate entity) {
        if (entity == null) {
            return null;
        }

        return new ShippingRateDto(
            entity.getId(),
            entity.getRegionType().name(),
            entity.getWeightFrom(),
            entity.getWeightTo(),
            entity.getPrice(),
            entity.getUnit(),
            entity.getExtraPrice()
        );
    }
}