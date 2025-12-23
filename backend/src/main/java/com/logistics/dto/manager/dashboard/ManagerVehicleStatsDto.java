package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerVehicleStatsDto {
    private long total;
    private long available;
    private long inUse;
    private long maintenance;
    private long archived;
}