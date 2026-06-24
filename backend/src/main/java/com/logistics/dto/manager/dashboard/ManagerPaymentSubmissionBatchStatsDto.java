package com.logistics.dto.manager.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ManagerPaymentSubmissionBatchStatsDto {
    private Long total = 0L;
    private Long processing = 0L;
    private Long completed = 0L;

    public ManagerPaymentSubmissionBatchStatsDto(
            Long total,
            Long processing,
            Long completed) {
        this.total = total;
        this.processing = processing;
        this.completed = completed;
    }
}