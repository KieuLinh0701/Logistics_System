package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ManagerVehicleStatsDto {
    private Long total = 0L;
    private Long available = 0L;
    private Long inUse = 0L;
    private Long maintenance = 0L;
    private Long archived = 0L;

    public ManagerVehicleStatsDto(Long total, Long available, Long inUse, Long maintenance, Long archived) {
        this.total = total;
        this.available = available;
        this.inUse = inUse;
        this.maintenance = maintenance;
        this.archived = archived;
    }
}