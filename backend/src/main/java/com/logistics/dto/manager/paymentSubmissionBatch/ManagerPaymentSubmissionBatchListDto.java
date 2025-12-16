package com.logistics.dto.manager.paymentSubmissionBatch;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerPaymentSubmissionBatchListDto {
    private Integer id;
    private String code;
    private User shipper;
    private BigDecimal totalSystemAmount;
    private BigDecimal totalActualAmount;
    private String status;
    private User checkedBy;
    private LocalDateTime checkedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String notes;
    private Integer totalOrders;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User {
        private String lastName;
        private String firstName;
        private String phoneNumber;
    }
}
