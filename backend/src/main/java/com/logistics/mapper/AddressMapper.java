package com.logistics.mapper;

import com.logistics.dto.AddressDto;
import com.logistics.entity.Address;

public class AddressMapper {

    public static AddressDto toDto(Address entity) {
        if (entity == null) {
            return null;
        }

        return new AddressDto(
                entity.getId(),
                entity.getWardCode(),
                entity.getCityCode(),
                entity.getDetail(),
                Boolean.TRUE.equals(entity.getIsDefault()),
                entity.getName(),
                entity.getPhoneNumber());
    }
}