package com.logistics.dto.manager.ai;

import com.logistics.enums.AiRoutePlanStatus;
import com.logistics.enums.RouteMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ManagerAiRoutePlanSummaryDto {
    private Long id;
    private String planCode;
    private AiRoutePlanStatus status;
    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationMinutes;
    private BigDecimal totalFuelCost;
    private Long totalCod;
    private Integer unassignedCount;
    private Integer routeCount;
    private RouteMode routeMode;
    private Boolean returnToOffice;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
