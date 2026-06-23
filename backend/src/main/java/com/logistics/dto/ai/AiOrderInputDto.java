package com.logistics.dto.ai;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiOrderInputDto {
    private Integer id;
    private String trackingNumber;
    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private Integer recipientWardCode;
    private Integer recipientCityCode;
    private Double latitude;
    private Double longitude;
    private Integer codAmount;
    private String priority;
    private Double weightKg;
}
