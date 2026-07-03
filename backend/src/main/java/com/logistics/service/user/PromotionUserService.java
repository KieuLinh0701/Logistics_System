package com.logistics.service.user;

import com.logistics.dto.user.UserPromotionDto;
import com.logistics.entity.Promotion;
import com.logistics.request.user.promotion.PromotionUserRequest;
import com.logistics.response.ListResponse;

import java.math.BigDecimal;
import java.util.Optional;

public interface PromotionUserService {
    ListResponse<UserPromotionDto> getActiveUserPromotions(Integer userId, PromotionUserRequest req);
    boolean canUsePromotion(Integer userId, Integer promotionId, Integer serviceTypeId, Integer orderValue, BigDecimal weight);
    Optional<Promotion> findById(Integer id);
    int calculateDiscount(Promotion promotion, int serviceFee);
    void increaseUsage(Integer promotionId, Integer userId);
    void decreaseUsage(Integer promotionId, Integer userId);
}