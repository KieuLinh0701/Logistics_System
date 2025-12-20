package com.logistics.service.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.user.dashboard.UserCreatedOrderCountDTO;
import com.logistics.dto.user.dashboard.UserDashboardChartResponseDTO;
import com.logistics.dto.user.dashboard.UserDashboardOverviewResponseDTO;
import com.logistics.dto.user.dashboard.UserDeliveredOrderCountDTO;
import com.logistics.dto.user.dashboard.UserOrderStatsDTO;
import com.logistics.dto.user.dashboard.UserOrderTimelineDTO;
import com.logistics.dto.user.dashboard.UserProductStatsDTO;
import com.logistics.dto.user.dashboard.UserProductTypeCountDto;
import com.logistics.dto.user.dashboard.UserRevenueStatsDTO;
import com.logistics.dto.user.dashboard.UserTopProductItemDto;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ProductType;
import com.logistics.repository.OrderProductRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ProductRepository;
import com.logistics.request.SearchRequest;
import com.logistics.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardUserService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SettlementBatchUserService settlementBatchUserService;
    private final OrderProductRepository orderProductRepository;

    public ApiResponse<UserDashboardOverviewResponseDTO> getOverview(Integer userId) {
        try {
            List<Object[]> rows = productRepository.countProductsByTypeForUser(userId);
            Map<ProductType, Long> productCounts = new EnumMap<>(ProductType.class);

            for (ProductType type : ProductType.values()) {
                productCounts.put(type, 0L);
            }

            if (rows != null) {
                for (Object[] row : rows) {
                    if (row != null && row[0] instanceof ProductType && row[1] instanceof Long) {
                        ProductType type = (ProductType) row[0];
                        Long count = (Long) row[1];
                        productCounts.put(type, count);
                    }
                }
            }

            UserOrderStatsDTO orders = orderRepository.getUserOrderStats(userId);
            UserProductStatsDTO products = productRepository.getUserProductStats(userId);
            UserRevenueStatsDTO revenue = settlementBatchUserService.getUserRevenueStats(userId);

            UserDashboardOverviewResponseDTO data = new UserDashboardOverviewResponseDTO();
            data.setOrders(orders);
            data.setProducts(products);
            data.setRevenue(revenue);
            data.setProductCounts(productCounts);

            return new ApiResponse<>(true, "Lấy thông tin tổng quan thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy tổng quan: " + e.getMessage(), null);
        }
    }

    public ApiResponse<UserDashboardChartResponseDTO> getChart(Integer userId, SearchRequest request) {
        try {
            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            List<UserTopProductItemDto> topSelling = orderProductRepository.findTopSellingProducts(
                    userId,
                    OrderStatus.DELIVERED,
                    startDate,
                    endDate,
                    PageRequest.of(0, 5));

            List<UserTopProductItemDto> topReturned = orderProductRepository.findTopReturnedProducts(
                    userId,
                    List.of(OrderStatus.RETURNING, OrderStatus.RETURNED),
                    startDate,
                    endDate,
                    PageRequest.of(0, 5));

            List<UserOrderTimelineDTO> orderTimelineDTOs = getOrderTimeline(userId, startDate,
                    endDate);

            UserDashboardChartResponseDTO data = new UserDashboardChartResponseDTO();
            data.setTopSelling(topSelling);
            data.setTopReturned(topReturned);
            data.setOrderTimelines(orderTimelineDTOs);

            return new ApiResponse<>(true, "Lấy thông tin biểu đồ thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy biểu đồ: " + e.getMessage(), null);
        }
    }

    public List<UserOrderTimelineDTO> getOrderTimeline(
            Integer userId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        List<UserCreatedOrderCountDTO> createdList = orderRepository.countCreatedOrdersByDate(userId, startDate,
                endDate);

        List<UserDeliveredOrderCountDTO> deliveredList = orderRepository.countDeliveredOrdersByDate(userId, startDate,
                endDate);

        Map<LocalDate, UserOrderTimelineDTO> map = new TreeMap<>();

        for (UserCreatedOrderCountDTO c : createdList) {
            LocalDate date = c.getDate().toLocalDate();
            map.put(
                    date,
                    new UserOrderTimelineDTO(date, c.getCreatedCount(), 0L));
        }

        for (UserDeliveredOrderCountDTO d : deliveredList) {
            LocalDate date = d.getDate().toLocalDate();
            map.compute(
                    date,
                    (k, v) -> {
                        if (v == null) {
                            return new UserOrderTimelineDTO(date, 0L, d.getDeliveredCount());
                        }
                        v.setDeliveredCount(d.getDeliveredCount());
                        return v;
                    });
        }

        return new ArrayList<>(map.values());
    }
}