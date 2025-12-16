package com.logistics.dto.manager.paymentSubmission;

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
public class ManagerPaymentSubmissionListDto {
    private Integer id;
    private String code;
    private BigDecimal systemAmount;
    private BigDecimal actualAmount;
    private Order order;
    private String status;
    private User checkedBy;
    private LocalDateTime checkedAt;
    private LocalDateTime paidAt;
    private LocalDateTime updatedAt;
    private String notes;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Order {
        private String trackingNumber;
    }

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
