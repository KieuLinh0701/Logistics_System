package com.logistics.service.common;

import com.logistics.dto.common.PublicPromotionDto;
import com.logistics.request.common.promotion.PromotionPublicRequest;
import com.logistics.response.ListResponse;

public interface PromotionPublicService {
    ListResponse<PublicPromotionDto> getActivePromotions(PromotionPublicRequest request);
}