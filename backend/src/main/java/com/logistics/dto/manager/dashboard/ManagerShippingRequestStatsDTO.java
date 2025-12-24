package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ManagerShippingRequestStatsDTO {
    private Long total = 0L;
    private Long pending = 0L;
    private Long processing = 0L;
    private Long resolved = 0L;
    private Long rejected = 0L;
    private Long cancelled = 0L;

    public ManagerShippingRequestStatsDTO(Long total, Long pending, Long processing,
                                          Long resolved, Long rejected, Long cancelled) {
        this.total = total;
        this.pending = pending;
        this.processing = processing;
        this.resolved = resolved;
        this.rejected = rejected;
        this.cancelled = cancelled;
    }
}