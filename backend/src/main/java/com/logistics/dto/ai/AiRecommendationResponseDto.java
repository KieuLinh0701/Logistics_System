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
public class AiRecommendationResponseDto {
    private Boolean success;
    private String message;

    @Builder.Default
    private List<AiRecommendationItemDto> recommendations = new ArrayList<>();

    @JsonProperty("fallback_location_source")
    @JsonAlias("fallbackLocationSource")
    private String fallbackLocationSource;

    @JsonProperty("location_source")
    @JsonAlias("locationSource")
    private String locationSource;
}