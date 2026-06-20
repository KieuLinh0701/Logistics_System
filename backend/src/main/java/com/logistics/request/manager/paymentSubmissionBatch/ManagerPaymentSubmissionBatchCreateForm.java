package com.logistics.request.manager.paymentSubmissionBatch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManagerPaymentSubmissionBatchCreateForm {
    private Integer shipperId;
    private BigDecimal totalActualAmount;
}
