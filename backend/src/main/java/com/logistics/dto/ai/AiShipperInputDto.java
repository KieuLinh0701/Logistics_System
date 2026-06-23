package com.logistics.dto.ai;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiShipperInputDto {
    private Integer id;
    private Integer employeeId;
    private String name;
    private Integer capacity;
    private Double speedKmh;
    private Double fuelCostPerKm;
    private String startTime;
    private String vehicleType;
    private Integer maxWeightKg;
    private Double remainingWeightKg;
    private Integer batteryLevel;
    @Builder.Default
    private List<AiShipperAssignmentAreaDto> assignments = new ArrayList<>();
}
