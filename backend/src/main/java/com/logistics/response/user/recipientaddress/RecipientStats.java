package com.logistics.response.user.recipientaddress;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecipientStats {

    private long totalSystemOrders;
    private double successRate;
    private double returnedRate;
    private LocalDateTime latestOrderDate;
}