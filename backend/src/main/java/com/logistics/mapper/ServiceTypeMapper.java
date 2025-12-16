package com.logistics.mapper;

import java.util.List;

import com.logistics.dto.ServiceTypeDto;
import com.logistics.dto.ServiceTypeWithRateDto;
import com.logistics.dto.ShippingRateDto;
import com.logistics.entity.ServiceType;

public class ServiceTypeMapper {

    public static ServiceTypeDto toDto(ServiceType entity) {
        if (entity == null) {
            return null;
        }

        return new ServiceTypeDto(
                entity.getId(),
                entity.getName(),
                entity.getDeliveryTime(),
                entity.getDescription());
    }

    public static ServiceTypeWithRateDto toDtoWithRate(ServiceType entity) {
        if (entity == null) {
            return null;
        }

        List<ShippingRateDto> rateDtos = entity.getRates()
                .stream()
                .map(ShippingRateMapper::toDto)
                .toList();

        return new ServiceTypeWithRateDto(
                entity.getId(),
                entity.getName(),
                entity.getDeliveryTime(),
                rateDtos);
    }
}