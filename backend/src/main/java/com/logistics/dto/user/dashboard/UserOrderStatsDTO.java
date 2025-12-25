package com.logistics.dto.user.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class UserOrderStatsDTO {
    private Long total = 0L;
    private Long draft = 0L;
    private Long pending = 0L;
    private Long confirmed = 0L;
    private Long readyForPickup = 0L;
    private Long pickingUp = 0L;
    private Long shipping = 0L;
    private Long delivering = 0L;
    private Long delivered = 0L;
    private Long failedDelivery = 0L;
    private Long returning = 0L;
    private Long returnedCancelled = 0L;

    public UserOrderStatsDTO(Long total, Long draft, Long pending, Long confirmed,
                             Long readyForPickup, Long pickingUp, Long shipping,
                             Long delivering, Long delivered, Long failedDelivery,
                             Long returning, Long returnedCancelled) {
        this.total = total;
        this.draft = draft;
        this.pending = pending;
        this.confirmed = confirmed;
        this.readyForPickup = readyForPickup;
        this.pickingUp = pickingUp;
        this.shipping = shipping;
        this.delivering = delivering;
        this.delivered = delivered;
        this.failedDelivery = failedDelivery;
        this.returning = returning;
        this.returnedCancelled = returnedCancelled;
    }
}