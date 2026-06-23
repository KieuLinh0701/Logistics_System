package com.logistics.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiShipperRouteOutputDto {
    @JsonProperty("shipper_id")
    @JsonAlias({"shipperId"})
    private Integer shipperId;

    @JsonProperty("employee_id")
    @JsonAlias({"employeeId"})
    private Integer employeeId;

    @JsonProperty("shipper_name")
    @JsonAlias({"shipperName"})
    private String shipperName;

    @JsonProperty("route_sequence")
    @JsonAlias({"routeSequence"})
    private Integer routeSequence;

    private List<AiRouteStopOutputDto> stops = new ArrayList<>();

    @JsonProperty("return_to_office_stop")
    @JsonAlias({"returnToOfficeStop"})
    private AiRouteStopOutputDto returnToOfficeStop;

    @JsonProperty("estimated_distance_km")
    @JsonAlias({"estimatedDistanceKm"})
    private Double estimatedDistanceKm;

    @JsonProperty("estimated_duration_minutes")
    @JsonAlias({"estimatedDurationMinutes"})
    private Double estimatedDurationMinutes;

    @JsonProperty("fuel_cost")
    @JsonAlias({"fuelCost"})
    private Double fuelCost;

    @JsonProperty("total_cod")
    @JsonAlias({"totalCod"})
    private Long totalCod;

    @JsonProperty("encoded_polyline")
    @JsonAlias({"encodedPolyline"})
    private String encodedPolyline;

    @JsonProperty("start_time")
    @JsonAlias({"startTime"})
    private String startTime;

    @JsonProperty("route_mode")
    @JsonAlias({"routeMode"})
    private String routeMode;

    @JsonProperty("return_to_office")
    @JsonAlias({"returnToOffice"})
    private Boolean returnToOffice;

    @JsonProperty("matrix_source")
    @JsonAlias({"matrixSource"})
    private String matrixSource;

    @JsonProperty("fallback_used")
    @JsonAlias({"fallbackUsed"})
    private Boolean fallbackUsed;

    @JsonProperty("optimizer_duration_seconds")
    @JsonAlias({"optimizerDurationSeconds"})
    private Integer optimizerDurationSeconds;

    @JsonProperty("cost_mode")
    @JsonAlias({"costMode"})
    private String costMode;
}
