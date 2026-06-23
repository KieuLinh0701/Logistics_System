package com.logistics.dto.manager.ai;

import com.logistics.enums.AiRoutePlanStatus;
import com.logistics.enums.RouteMode;
import com.logistics.enums.RouteOptimizationScope;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ManagerAiRoutePlanDetailDto {
    private Long id;
    private String planCode;
    private AiRoutePlanStatus status;
    private Integer officeId;
    private String officeName;
    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationMinutes;
    private BigDecimal totalFuelCost;
    private Long totalCod;
    private Integer unassignedCount;
    private String optimizationNote;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private RouteMode routeMode;
    private Boolean returnToOffice;
    private RouteOptimizationScope optimizationScope;
    private Integer versionNumber;
    private Boolean active;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private List<ManagerAiShipperRouteDto> routes;
    private List<ManagerAiUnassignedOrderDto> unassignedOrders;
}
