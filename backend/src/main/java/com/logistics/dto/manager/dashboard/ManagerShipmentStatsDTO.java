package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShipmentStatsDTO {
    private long total;
    private long pending;
    private long inTransit;
    private long completed;
    private long cancelled;
}
