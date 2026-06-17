package com.logistics.dto.ai.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiRouteOptimizationResponseDto {
    private Boolean success;
    private String message;
    private List<AiShipperRouteOutputDto> routes = new ArrayList<>();
    private List<AiUnassignedOrderOutputDto> unassignedOrders = new ArrayList<>();
    private AiOptimizationSummaryDto summary;
}
