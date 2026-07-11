package com.logistics.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class AiRecommendationRequestDto {

    @JsonProperty("shipperId")
    private Integer shipperId;

    private AiRecommendationLocationDto currentLocation;

    private AiRecommendationLoadDto currentLoad;

    private AiRecommendationVehicleCapacityDto vehicleCapacity;

    @Builder.Default
    private List<AiRecommendationCurrentOrderDto> currentOrders = new ArrayList<>();

    @Builder.Default
    private List<AiRecommendationCandidateOrderDto> candidateOrders = new ArrayList<>();
}
