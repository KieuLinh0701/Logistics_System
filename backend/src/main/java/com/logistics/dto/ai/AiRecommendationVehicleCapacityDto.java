package com.logistics.dto.ai;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiRecommendationVehicleCapacityDto {
    @Builder.Default
    private Double weight = 0.0;
    @Builder.Default
    private Double volume = 0.0;
    @Builder.Default
    private Integer maxOrders = 0;
    @Builder.Default
    private Integer currentOrders = 0;
}
