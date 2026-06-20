package com.logistics.request.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentSubmissionRequest {
    private String status;
    private String notes;
    private java.math.BigDecimal actualAmount;
}
