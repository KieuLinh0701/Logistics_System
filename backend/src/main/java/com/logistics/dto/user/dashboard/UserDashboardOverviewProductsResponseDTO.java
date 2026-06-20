package com.logistics.dto.user.dashboard;

import com.logistics.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDashboardOverviewProductsResponseDTO {
    private UserProductStatsDTO products;
    private Map<ProductType, Long> productCounts;
}
