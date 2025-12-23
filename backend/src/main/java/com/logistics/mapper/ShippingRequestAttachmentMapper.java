package com.logistics.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.logistics.dto.ShippingRequestAttachmentDto;
import com.logistics.entity.ShippingRequestAttachment;

public class ShippingRequestAttachmentMapper {

    public static ShippingRequestAttachmentDto toDto(ShippingRequestAttachment entity) {
        if (entity == null) {
            return null;
        }

        return new ShippingRequestAttachmentDto(
                entity.getId(),
                entity.getFileName(),
                entity.getUrl());
    }

    public static List<ShippingRequestAttachmentDto> toDtoList(List<ShippingRequestAttachment> entities) {
        if (entities == null)
            return Collections.emptyList();
        return entities.stream()
                .map(ShippingRequestAttachmentMapper::toDto)
                .collect(Collectors.toList());
    }
}