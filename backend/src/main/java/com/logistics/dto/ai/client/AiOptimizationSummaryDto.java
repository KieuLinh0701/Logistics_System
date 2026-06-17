package com.logistics.dto.ai.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiOptimizationSummaryDto {
    private Double totalDistanceKm;
    private Double totalDurationMinutes;
    private Double totalFuelCost;
    private Long totalCod;
    private Integer assignedOrderCount;
    private Integer unassignedOrderCount;
    private Integer shipperCount;
}
