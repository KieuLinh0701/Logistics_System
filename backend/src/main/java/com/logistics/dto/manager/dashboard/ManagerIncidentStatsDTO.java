package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ManagerIncidentStatsDTO {
    private Long total = 0L;
    private Long pendingLow = 0L;
    private Long pendingMedium = 0L;
    private Long pendingHight = 0L;
    private Long pending = 0L;
    private Long processing = 0L;
    private Long resolved = 0L;
    private Long rejected = 0L;

    public ManagerIncidentStatsDTO(Long total, Long pendingLow, Long pendingMedium, Long pendingHight,
                                   Long pending, Long processing, Long resolved, Long rejected) {
        this.total = total;
        this.pendingLow = pendingLow;
        this.pendingMedium = pendingMedium;
        this.pendingHight = pendingHight;
        this.pending = pending;
        this.processing = processing;
        this.resolved = resolved;
        this.rejected = rejected;
    }
}