package com.logistics.service.admin;

import com.logistics.request.admin.CreatePromotionRequest;
import com.logistics.request.admin.UpdatePromotionRequest;

import java.util.Map;

public interface PromotionAdminService {

    Map<String, Object> listPromotions(int page, int limit, String search, String status, Boolean isGlobal);

    Map<String, Object> getPromotionById(Integer promotionId);

    void createPromotion(CreatePromotionRequest request);

    void updatePromotion(Integer promotionId, UpdatePromotionRequest request);

    void deletePromotion(Integer promotionId);
}
