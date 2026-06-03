package com.logistics.dto.ai.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiShipperRouteOutputDto {
    private Integer shipperId;
    private Integer employeeId;
    private String shipperName;
    private Integer routeSequence;
    private List<AiRouteStopOutputDto> stops = new ArrayList<>();
    private Double estimatedDistanceKm;
    private Double estimatedDurationMinutes;
    private Double fuelCost;
    private Long totalCod;
    private String encodedPolyline;
    private String startTime;
}
