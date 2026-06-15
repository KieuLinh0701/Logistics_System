package com.logistics.dto.user.dashboard;

import java.util.List;
import java.util.Map;

import com.logistics.enums.ProductType;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardOverviewProductsResponseDTO {
    private UserProductStatsDTO products;
    private Map<ProductType, Long> productCounts;
}
