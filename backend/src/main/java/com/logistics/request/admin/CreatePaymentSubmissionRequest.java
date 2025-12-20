package com.logistics.request.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentSubmissionRequest {
    private String status;
    private String notes;
    private java.math.BigDecimal actualAmount;
}
