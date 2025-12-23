package com.logistics.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.logistics.dto.OrderHistoryDto;
import com.logistics.entity.OrderHistory;

public class OrderHistoryMapper {

    public static OrderHistoryDto toDto(OrderHistory entity) {
        if (entity == null) {
            return null;
        }

        return new OrderHistoryDto(
                entity.getFromOffice() != null ? entity.getFromOffice().getName() : null,
                entity.getToOffice() != null ? entity.getToOffice().getName() : null,
                entity.getAction().name(),
                entity.getNote(),
                entity.getActionTime());
    }

    public static List<OrderHistoryDto> toDtoList(List<OrderHistory> entities) {
        if (entities == null)
            return Collections.emptyList();
        return entities.stream()
                .map(OrderHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
}