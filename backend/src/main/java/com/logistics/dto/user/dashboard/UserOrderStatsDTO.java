package com.logistics.dto.user.dashboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderStatsDTO {
    private long total;
    private long draft;
    private long pending;
    private long confirmed;
    private long readyForPickup;
    private long pickingUp;
    private long shipping;
    private long delivering;
    private long delivered;
    private long failedDelivery;
    private long returning;
    private long returnedCancelled;
}
