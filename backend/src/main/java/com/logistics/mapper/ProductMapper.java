package com.logistics.mapper;

import com.logistics.dto.ProductDto;
import com.logistics.entity.Product;

public class ProductMapper {

    public static ProductDto toDto(Product entity) {
        if (entity == null) {
            return null;
        }

        return new ProductDto(
                entity.getId(),
                entity.getCode(),
                entity.getImage(),
                entity.getName(),
                entity.getWeight(),
                entity.getPrice(),
                entity.getType().name(),
                entity.getStatus().name(),
                entity.getStock(),
                entity.getSoldQuantity(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}