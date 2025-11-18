package com.logistics.mapper;

import com.logistics.dto.PromotionDto;
import com.logistics.entity.Promotion;

public class PromotionMapper {

    public static PromotionDto toDto(Promotion entity) {
        if (entity == null) {
            return null;
        }

        return new PromotionDto(
                entity.getId(),
                entity.getCode(),
                entity.getDescription(),
                entity.getDiscountType().name(),
                entity.getDiscountValue(),
                entity.getMinOrderValue(),
                entity.getMaxDiscountAmount(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getUsageLimit(),
                entity.getUsedCount(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}