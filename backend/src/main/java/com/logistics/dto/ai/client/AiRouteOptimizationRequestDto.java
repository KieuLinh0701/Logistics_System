package com.logistics.dto.ai.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiRouteOptimizationRequestDto {
    private AiOfficeLocationDto office;
    @Builder.Default
    private List<AiShipperInputDto> shippers = new ArrayList<>();
    @Builder.Default
    private List<AiOrderInputDto> orders = new ArrayList<>();
    @Builder.Default
    private Map<String, Object> options = new HashMap<>();
}
