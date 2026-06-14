package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ManagerPaymentSubmissionBatchStatsDto {
    private Long total = 0L;
    private Long open = 0L;
    private Long processing = 0L;
    private Long completed = 0L;

    public ManagerPaymentSubmissionBatchStatsDto(
            Long total,
            Long open,
            Long processing,
            Long completed) {
        this.total = total;
        this.open = open;
        this.processing = processing;
        this.completed = completed;
    }
}