package com.logistics.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AiRouteStopOutputDto {
    @JsonProperty("order_id")
    @JsonAlias({"orderId"})
    private Integer orderId;

    @JsonProperty("tracking_number")
    @JsonAlias({"trackingNumber"})
    private String trackingNumber;

    @JsonProperty("recipient_name")
    @JsonAlias({"recipientName"})
    private String recipientName;

    @JsonProperty("recipient_phone")
    @JsonAlias({"recipientPhone"})
    private String recipientPhone;

    @JsonProperty("recipient_address")
    @JsonAlias({"recipientAddress"})
    private String recipientAddress;

    private Double latitude;
    private Double longitude;

    @JsonProperty("cod_amount")
    @JsonAlias({"codAmount"})
    private Integer codAmount;

    private String priority;

    @JsonProperty("stop_sequence")
    @JsonAlias({"stopSequence"})
    private Integer stopSequence;

    @JsonProperty("stop_type")
    @JsonAlias({"stopType"})
    private String stopType;

    @JsonProperty("eta_time")
    @JsonAlias({"etaTime"})
    private String etaTime;

    @JsonProperty("eta_minutes_from_start")
    @JsonAlias({"etaMinutesFromStart"})
    private Integer etaMinutesFromStart;

    @JsonProperty("leg_distance_km")
    @JsonAlias({"legDistanceKm"})
    private Double legDistanceKm;

    @JsonProperty("leg_duration_minutes")
    @JsonAlias({"legDurationMinutes"})
    private Integer legDurationMinutes;

    @JsonProperty("service_time_minutes")
    @JsonAlias({"serviceTimeMinutes"})
    private Integer serviceTimeMinutes;
}
