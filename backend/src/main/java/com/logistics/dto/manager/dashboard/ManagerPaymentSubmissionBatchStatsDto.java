package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ManagerPaymentSubmissionBatchStatsDto {
    private Long total = 0L;
    private Long pending = 0L;
    private Long checking = 0L;
    private Long completed = 0L;
    private Long partial = 0L;
    private Long cancelled = 0L;

    public ManagerPaymentSubmissionBatchStatsDto(Long total, Long pending, Long checking, Long completed,
                                                 Long partial, Long cancelled) {
        this.total = total;
        this.pending = pending;
        this.checking = checking;
        this.completed = completed;
        this.partial = partial;
        this.cancelled = cancelled;
    }
}