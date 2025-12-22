package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShippingRequestStatsDTO {
    private long total;
    private long pending;
    private long processing;
    private long resolved;
    private long rejected;
    private long cancelled;
}
