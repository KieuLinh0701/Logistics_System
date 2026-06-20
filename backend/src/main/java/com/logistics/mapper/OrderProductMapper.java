package com.logistics.mapper;

import com.logistics.dto.OrderProductDto;
import com.logistics.entity.OrderProduct;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrderProductMapper {

    public static OrderProductDto toDto(OrderProduct entity) {
        if (entity == null) {
            return null;
        }

        return new OrderProductDto(
                entity.getProduct().getId(),
                entity.getProduct().getPrice(),
                entity.getProduct().getType().name(),
                entity.getProduct().getStock(),
                entity.getProduct().getCode(),
                entity.getProduct().getName(),
                entity.getProduct().getWeight(),
                entity.getQuantity(),
                entity.getPrice(),
                entity.getDeliveredQuantity() != null ? entity.getDeliveredQuantity() : 0,
                entity.getReturnedQuantity() != null ? entity.getReturnedQuantity() : 0
        );
    }

    public static List<OrderProductDto> toDtoList(List<OrderProduct> entities) {
        if (entities == null)
            return Collections.emptyList();
        return entities.stream()
                .map(OrderProductMapper::toDto)
                .collect(Collectors.toList());
    }
}