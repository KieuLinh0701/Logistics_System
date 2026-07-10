package com.logistics.service.ai;

import com.logistics.dto.ai.AiRouteOptimizationRequestDto;
import com.logistics.dto.ai.AiRouteOptimizationResponseDto;

public interface AiServiceClient {

    AiRouteOptimizationResponseDto optimizeRoutes(AiRouteOptimizationRequestDto request);

    boolean isHealthy();
}
