package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ManagerOrderStatsDTO {
    private Long total = 0L;
    private Long pending = 0L;
    private Long confirmed = 0L;
    private Long readyForPickup = 0L;
    private Long pickingOrPicked = 0L;
    private Long inWarehouse = 0L;
    private Long customerAtOffice = 0L;
    private Long delivering = 0L;
    private Long delivered = 0L;
    private Long returned = 0L;
    private Long returning = 0L;
    private Long failedDelivery = 0L;

    public ManagerOrderStatsDTO(Long total, Long pending, Long confirmed, Long readyForPickup,
                                Long pickingOrPicked, Long inWarehouse, Long customerAtOffice,
                                Long delivering, Long delivered, Long returned, Long returning,
                                Long failedDelivery) {
        this.total = total;
        this.pending = pending;
        this.confirmed = confirmed;
        this.readyForPickup = readyForPickup;
        this.pickingOrPicked = pickingOrPicked;
        this.inWarehouse = inWarehouse;
        this.customerAtOffice = customerAtOffice;
        this.delivering = delivering;
        this.delivered = delivered;
        this.returned = returned;
        this.returning = returning;
        this.failedDelivery = failedDelivery;
    }
}