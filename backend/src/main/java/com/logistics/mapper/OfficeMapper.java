package com.logistics.mapper;

import com.logistics.dto.OfficeDto;
import com.logistics.entity.Office;

public class OfficeMapper {

    public static OfficeDto toDto(Office entity) {
        if (entity == null) {
            return null;
        }

        return new OfficeDto(
                entity.getId(),
                entity.getCode(),
                entity.getPostalCode(),
                entity.getName(),
                entity.getCityCode(),
                entity.getWardCode(),
                entity.getDetail(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getOpeningTime(),
                entity.getClosingTime(),
                entity.getType().name(),
                entity.getStatus().name(),
                entity.getCapacity(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}