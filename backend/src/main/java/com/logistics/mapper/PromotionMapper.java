package com.logistics.mapper;

import com.logistics.dto.common.PublicPromotionDto;
import com.logistics.dto.user.UserPromotionDto;
import com.logistics.entity.Promotion;

public class PromotionMapper {

    public static UserPromotionDto toUserPromotionDto(Promotion entity) {
        if (entity == null) {
            return null;
        }

        return new UserPromotionDto(
                entity.getId(),
                entity.getCode(),
                entity.getTitle(),
                entity.getDiscountType().name(),
                entity.getDiscountValue(),
                entity.getMaxDiscountAmount(),
                entity.getEndDate());
    }

    public static PublicPromotionDto toPublicPromotionDto(Promotion entity) {
        if (entity == null) {
            return null;
        }

        return new PublicPromotionDto(
                entity.getId(),
                entity.getCode(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDiscountType().name(),
                entity.getDiscountValue(),
                entity.getMaxDiscountAmount(),
                entity.getMinOrderValue(),
                entity.getUsedCount(),
                entity.getUsageLimit(),
                entity.getStartDate(),
                entity.getEndDate());
    }
}