package com.logistics.service.manager;

import com.logistics.dto.manager.ai.ManagerAiRoutePlanDetailDto;
import com.logistics.dto.manager.ai.ManagerAiRoutePlanSummaryDto;
import com.logistics.request.manager.ai.ManagerAiOptimizeRequest;

import java.util.List;
import java.util.Map;

public interface AiRouteOptimizationManagerService {
    Map<String, Object> previewDeliveryReadyOrders(Integer managerUserId);

    ManagerAiRoutePlanDetailDto optimize(Integer managerUserId, ManagerAiOptimizeRequest request);

    List<ManagerAiRoutePlanSummaryDto> listPlans(Integer managerUserId);

    ManagerAiRoutePlanDetailDto getPlan(Integer managerUserId, Long planId);

    ManagerAiRoutePlanDetailDto confirmPlan(Integer managerUserId, Long planId);

    void cancelPlan(Integer managerUserId, Long planId);
}