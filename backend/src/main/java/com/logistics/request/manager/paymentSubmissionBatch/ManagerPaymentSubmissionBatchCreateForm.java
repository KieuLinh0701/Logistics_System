package com.logistics.request.manager.paymentSubmissionBatch;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManagerPaymentSubmissionBatchCreateForm {
    private Integer shipperId;
    private BigDecimal totalActualAmount;
}
