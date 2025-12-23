package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerIncidentStatsDTO {
    private long total;
    private long pendingLow;
    private long pendingMedium;
    private long pendingHight;
    private long pending;
    private long processing;
    private long resolved;
    private long rejected;
}
