package com.logistics.dto.user.dashboard;

import java.util.List;

import com.logistics.enums.ProductType;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProductStatsDTO {
    private long total;
    private long outOfStock;
    private long lowStock;
    private long active;
}