package com.logistics.mapper;

import com.logistics.dto.serviceType.ServiceTypeDto;
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
            entity.getDescription(),
            entity.getStatus() != null ? entity.getStatus().name() : null,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}