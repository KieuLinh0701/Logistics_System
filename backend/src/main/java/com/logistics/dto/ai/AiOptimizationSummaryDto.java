package com.logistics.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AiOptimizationSummaryDto {
    @JsonProperty("total_distance_km")
    @JsonAlias({"totalDistanceKm"})
    private Double totalDistanceKm;

    @JsonProperty("total_duration_minutes")
    @JsonAlias({"totalDurationMinutes"})
    private Double totalDurationMinutes;

    @JsonProperty("total_fuel_cost")
    @JsonAlias({"totalFuelCost"})
    private Double totalFuelCost;

    @JsonProperty("total_cod")
    @JsonAlias({"totalCod"})
    private Long totalCod;

    @JsonProperty("assigned_order_count")
    @JsonAlias({"assignedOrderCount"})
    private Integer assignedOrderCount;

    @JsonProperty("unassigned_order_count")
    @JsonAlias({"unassignedOrderCount"})
    private Integer unassignedOrderCount;

    @JsonProperty("shipper_count")
    @JsonAlias({"shipperCount"})
    private Integer shipperCount;
}
