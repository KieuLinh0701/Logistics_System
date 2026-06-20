package com.logistics.mapper;

import com.logistics.dto.AddressDto;
import com.logistics.dto.AddressSummaryDto;
import com.logistics.entity.Address;
import com.logistics.response.user.recipientaddress.RecipientAddress;

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
                entity.getPhoneNumber(),
                entity.getFullAddress(),
                entity.getCityName(),
                entity.getWardName(),
                entity.getLatitude(),
                entity.getLongitude());
    }

    public static AddressSummaryDto toSummaryDto(
            String fullAddress,
            String name,
            String phone
    ) {
        return new AddressSummaryDto(
                fullAddress,
                name,
                phone
        );
    }

    public static RecipientAddress toRecipientAddress(Address entity) {
        if (entity == null) {
            return null;
        }

        return RecipientAddress.builder()
                .id(entity.getId())
                .name(entity.getName())
                .phoneNumber(entity.getPhoneNumber())
                .fullAddress(entity.getFullAddress())
                .cityCode(entity.getCityCode())
                .cityName(entity.getCityName())
                .wardCode(entity.getWardCode())
                .wardName(entity.getWardName())
                .detail(entity.getDetail())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .build();
    }
}