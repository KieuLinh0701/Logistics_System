package com.logistics.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationCandidateOrderDto {
    @JsonProperty("order_id")
    private Integer orderId;

    @JsonProperty("weight_kg")
    private Double weightKg;

    @JsonProperty("volume_m3")
    private Double volumeM3;

    private Double latitude;
    private Double longitude;

    @JsonProperty("recipient_ward_code")
    private Integer recipientWardCode;

    @JsonProperty("recipient_city_code")
    private Integer recipientCityCode;

    private String status;

    @JsonProperty("is_urgent")
    private Boolean isUrgent;
}