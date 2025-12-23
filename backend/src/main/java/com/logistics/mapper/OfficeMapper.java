package com.logistics.mapper;

import com.logistics.dto.OfficeDto;
import com.logistics.dto.common.PublicOfficeInformationDto;
import com.logistics.dto.common.PublicOfficeSearchDto;
import com.logistics.entity.Office;

public class OfficeMapper {

    public static PublicOfficeSearchDto toPublicOfficeSearchDto(Office entity) {
        if (entity == null) {
            return null;
        }

        return new PublicOfficeSearchDto(
                entity.getId(),
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
                entity.getType().name());
    }

    public static PublicOfficeInformationDto toPublicOfficeInformationDto(Office entity) {
        if (entity == null) {
            return null;
        }
 
        return new PublicOfficeInformationDto(
                entity.getId(),
                entity.getPostalCode(),
                entity.getName(),
                entity.getCityCode(),
                entity.getWardCode(),
                entity.getDetail(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getOpeningTime(),
                entity.getClosingTime());
    }

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