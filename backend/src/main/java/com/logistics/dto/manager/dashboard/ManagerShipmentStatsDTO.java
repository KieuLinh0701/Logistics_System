package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ManagerShipmentStatsDTO {
    private Long total = 0L;
    private Long pending = 0L;
    private Long inTransit = 0L;
    private Long completed = 0L;
    private Long cancelled = 0L;

    public ManagerShipmentStatsDTO(Long total, Long pending, Long inTransit, Long completed, Long cancelled) {
        this.total = total;
        this.pending = pending;
        this.inTransit = inTransit;
        this.completed = completed;
        this.cancelled = cancelled;
    }
}