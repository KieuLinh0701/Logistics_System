package com.logistics.service.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.logistics.dto.user.dashboard.UserDashboardChartProductResponseDTO;
import com.logistics.dto.user.dashboard.UserDashboardOverviewProductsResponseDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.user.dashboard.UserCreatedOrderCountDTO;
import com.logistics.dto.user.dashboard.UserDeliveredOrderCountDTO;
import com.logistics.dto.user.dashboard.UserOrderStatsDTO;
import com.logistics.dto.user.dashboard.UserOrderTimelineDTO;
import com.logistics.dto.user.dashboard.UserProductStatsDTO;
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
    private final UserUserService userService;

    public ApiResponse<UserDashboardOverviewProductsResponseDTO> getOverviewProducts(Integer userId) {
        try {
            Integer shopId = userService.getShopId(userId);

            List<Object[]> rows = productRepository.countProductsByTypeForUser(shopId);
            Map<ProductType, Long> productCounts = new EnumMap<>(ProductType.class);

            for (ProductType type : ProductType.values()) {
                productCounts.put(type, 0L);
            }

            if (rows != null) {
                for (Object[] row : rows) {
                    if (row != null && row[0] != null && row[1] != null) {
                        ProductType type;
                        try {
                            type = row[0] instanceof ProductType ? (ProductType) row[0]
                                    : ProductType.valueOf(row[0].toString());
                        } catch (IllegalArgumentException e) {
                            continue; // bỏ qua loại không hợp lệ
                        }
                        Long count = ((Number) row[1]).longValue();
                        productCounts.put(type, count);
                    }
                }
            }

            // Tổng quan sản phẩm
            UserProductStatsDTO products = productRepository.getUserProductStats(shopId);
            if (products == null) {
                products = new UserProductStatsDTO();
                products.setTotal(0L);
                products.setOutOfStock(0L);
                products.setLowStock(0L);
                products.setActive(0L);
            }

            UserDashboardOverviewProductsResponseDTO data = new UserDashboardOverviewProductsResponseDTO();
            data.setProducts(products);
            data.setProductCounts(productCounts);

            return new ApiResponse<>(true, "Lấy thông tin tổng quan sản pẩm thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy tổng quan sản phẩm: " + e.getMessage(), null);
        }
    }

    public ApiResponse<UserOrderStatsDTO> getOverviewOrders(Integer userId) {
        try {
            Integer shopId = userService.getShopId(userId);

            UserOrderStatsDTO orders = orderRepository.getUserOrderStats(shopId);
            if (orders == null) {
                orders = new UserOrderStatsDTO();
                orders.setTotal(0L);
                orders.setDraft(0L);
                orders.setPending(0L);
                orders.setConfirmed(0L);
                orders.setReadyForPickup(0L);
                orders.setPickingUp(0L);
                orders.setShipping(0L);
                orders.setDelivering(0L);
                orders.setDelivered(0L);
                orders.setFailedDelivery(0L);
                orders.setReturning(0L);
                orders.setReturnedCancelled(0L);
            }

            return new ApiResponse<>(true, "Lấy thông tin tổng quan đơn hàng thành công", orders);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy tổng quan đơn hàng: " + e.getMessage(), null);
        }
    }

    public ApiResponse<UserRevenueStatsDTO> getOverviewRevenue(Integer userId) {
        try {
            Integer shopId = userService.getShopId(userId);

            UserRevenueStatsDTO revenue = settlementBatchUserService.getUserRevenueStats(shopId);
            if (revenue == null) {
                revenue = new UserRevenueStatsDTO();
                revenue.setReceived(BigDecimal.ZERO);
                revenue.setNextSettlement(BigDecimal.ZERO);
                revenue.setPendingDebt(BigDecimal.ZERO);
                revenue.setNextSettlementDate("");
            }

            return new ApiResponse<>(true, "Lấy thông tin tổng quan thành công", revenue);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy tổng quan: " + e.getMessage(), null);
        }
    }

    public ApiResponse<UserDashboardChartProductResponseDTO> getChartProducts(
            Integer userId,
            SearchRequest request) {
        try {
            Integer shopId = userService.getShopId(userId);

            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            List<UserTopProductItemDto> topSelling = orderProductRepository.findTopSellingProducts(
                    shopId,
                    OrderStatus.DELIVERED,
                    startDate,
                    endDate,
                    PageRequest.of(0, 5));

            List<UserTopProductItemDto> topReturned = orderProductRepository.findTopReturnedProducts(
                    shopId,
                    List.of(OrderStatus.RETURNING, OrderStatus.RETURNED),
                    startDate,
                    endDate,
                    PageRequest.of(0, 5));

            UserDashboardChartProductResponseDTO data = new UserDashboardChartProductResponseDTO();
            data.setTopSelling(topSelling);
            data.setTopReturned(topReturned);

            return new ApiResponse<>(true, "Lấy thông tin biểu đồ thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy biểu đồ: " + e.getMessage(), null);
        }
    }

    public ApiResponse<List<UserOrderTimelineDTO>> getChartOrders(Integer userId,
            SearchRequest request) {
        try {
            Integer shopId = userService.getShopId(userId);

            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            List<UserOrderTimelineDTO> orderTimelineDTOs = getOrderTimeline(
                    shopId,
                    startDate,
                    endDate);

            return new ApiResponse<>(true, "Lấy thông tin biểu đồ thành công", orderTimelineDTOs);
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