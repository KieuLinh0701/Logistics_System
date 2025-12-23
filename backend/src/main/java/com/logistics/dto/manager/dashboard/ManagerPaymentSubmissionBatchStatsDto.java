package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerPaymentSubmissionBatchStatsDto {
    private long total;
    private long pending;
    private long checking;
    private long completed;
    private long partial;
    private long cancelled;
}