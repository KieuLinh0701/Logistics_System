package com.logistics.dto.user.dashboard;

import java.util.List;
import java.util.Map;

import com.logistics.enums.ProductType;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardOverviewResponseDTO {
    private UserOrderStatsDTO orders;
    private UserProductStatsDTO products;
    private UserRevenueStatsDTO revenue;
    private Map<ProductType, Long> productCounts;
}
