package com.logistics.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiRecommendationItemDto {
    @JsonProperty("order_id")
    @JsonAlias("orderId")
    private Integer orderId;

    private Integer score;

    private String level;

    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    @JsonProperty("estimated_distance_km")
    @JsonAlias("estimatedDistanceKm")
    private Double estimatedDistanceKm;

    @JsonProperty("estimated_duration_minutes")
    @JsonAlias("estimatedDurationMinutes")
    private Integer estimatedDurationMinutes;

    private Boolean recommended;
}