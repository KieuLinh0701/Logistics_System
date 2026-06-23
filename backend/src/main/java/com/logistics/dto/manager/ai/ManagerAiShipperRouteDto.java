package com.logistics.dto.manager.ai;

import com.logistics.enums.RouteMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ManagerAiShipperRouteDto {
    private Long routeId;
    private Integer shipperUserId;
    private Integer shipperEmployeeId;
    private String shipperName;
    private Integer routeSequence;
    private BigDecimal estimatedDistanceKm;
    private BigDecimal estimatedDurationMinutes;
    private BigDecimal fuelCost;
    private Long totalCod;
    private String encodedPolyline;
    private String startTime;
    private Integer stopCount;
    private RouteMode routeMode;
    private Boolean returnToOffice;
    private Integer routeVersion;
    private Long parentRouteId;
    private Boolean isActive;
    private List<ManagerAiRouteStopDto> stops;
    private ManagerAiRouteStopDto returnToOfficeStop;
}
