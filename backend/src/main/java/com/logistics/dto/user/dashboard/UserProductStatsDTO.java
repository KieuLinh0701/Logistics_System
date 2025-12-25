package com.logistics.dto.user.dashboard;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
public class UserProductStatsDTO {
    private Long total = 0L;
    private Long outOfStock = 0L;
    private Long lowStock = 0L;
    private Long active = 0L;

    public UserProductStatsDTO(Long total, Long outOfStock, Long lowStock, Long active) {
        this.total = total;
        this.outOfStock = outOfStock;
        this.lowStock = lowStock;
        this.active = active;
    }
}