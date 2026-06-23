package com.logistics.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiRouteOptimizationResponseDto {
    @JsonProperty("success")
    @JsonAlias({"Success"})
    private Boolean success;

    @JsonProperty("message")
    @JsonAlias({"Message"})
    private String message;

    private List<AiShipperRouteOutputDto> routes = new ArrayList<>();

    @JsonProperty("unassigned_orders")
    @JsonAlias({"unassignedOrders"})
    private List<AiUnassignedOrderOutputDto> unassignedOrders = new ArrayList<>();

    private AiOptimizationSummaryDto summary;
}
