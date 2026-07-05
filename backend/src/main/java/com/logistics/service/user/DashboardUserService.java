package com.logistics.service.user;

import com.logistics.dto.user.dashboard.*;
import com.logistics.request.SearchRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface DashboardUserService {
    UserDashboardOverviewProductsResponseDTO getOverviewProducts(Integer userId);
    UserOrderStatsDTO getOverviewOrders(Integer userId);
    UserRevenueStatsDTO getOverviewRevenue(Integer userId);
    UserDashboardChartProductResponseDTO getChartProducts(Integer userId, SearchRequest request);
    List<UserOrderTimelineDTO> getChartOrders(Integer userId, SearchRequest request);
    List<UserOrderTimelineDTO> getOrderTimeline(Integer userId, LocalDateTime startDate, LocalDateTime endDate);
}