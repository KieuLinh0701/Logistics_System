package com.logistics.service.user.impl;

import com.logistics.dto.user.dashboard.*;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ProductType;
import com.logistics.repository.OrderProductRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ProductRepository;
import com.logistics.request.SearchRequest;
import com.logistics.service.user.DashboardUserService;
import com.logistics.service.user.SettlementBatchUserService;
import com.logistics.service.user.UserUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardUserServiceImpl implements DashboardUserService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SettlementBatchUserService settlementBatchUserService;
    private final OrderProductRepository orderProductRepository;
    private final UserUserService userService;

    @Override
    @Transactional(readOnly = true)
    public UserDashboardOverviewProductsResponseDTO getOverviewProducts(Integer userId) {
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
                            continue;
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

            return data;
    }

    @Override
    @Transactional(readOnly = true)
    public UserOrderStatsDTO getOverviewOrders(Integer userId) {
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
                orders.setReturning(0L);
                orders.setReturnedCancelled(0L);
            }

            return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public UserRevenueStatsDTO getOverviewRevenue(Integer userId) {
            Integer shopId = userService.getShopId(userId);

            UserRevenueStatsDTO revenue = settlementBatchUserService.getUserRevenueStats(shopId);
            if (revenue == null) {
                revenue = new UserRevenueStatsDTO();
                revenue.setReceived(BigDecimal.ZERO);
                revenue.setNextSettlement(BigDecimal.ZERO);
                revenue.setPendingDebt(BigDecimal.ZERO);
                revenue.setNextSettlementDate("");
            }

            return revenue;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDashboardChartProductResponseDTO getChartProducts(
            Integer userId,
            SearchRequest request) {
            Integer shopId = userService.getShopId(userId);

            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            List<UserTopProductItemDto> topSelling = orderProductRepository.findTopSellingProducts(
                    shopId,
                    List.of(OrderStatus.DELIVERED, OrderStatus.DELIVERING, OrderStatus.DELIVERY_RETRY, OrderStatus.DELIVERY_FAILED_FINAL),
                    startDate,
                    endDate);

            List<UserTopProductItemDto> topReturned = orderProductRepository.findTopReturnedProducts(
                    shopId,
                    List.of(OrderStatus.RETURNING, OrderStatus.RETURN_READY_FOR_PICKUP, OrderStatus.RETURN_PICKED_UP, OrderStatus.RETURNED),
                    startDate,
                    endDate);

            UserDashboardChartProductResponseDTO data = new UserDashboardChartProductResponseDTO();
            data.setTopSelling(topSelling);
            data.setTopReturned(topReturned);

            return data;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOrderTimelineDTO> getChartOrders(Integer userId,
            SearchRequest request) {
            Integer shopId = userService.getShopId(userId);

            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate())
                    : null;

            return getOrderTimeline(
                    shopId,
                    startDate,
                    endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserOrderTimelineDTO> getOrderTimeline(
            Integer userId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        List<UserCreatedOrderCountDTO> createdList = orderRepository.countCreatedOrdersByDate(userId, startDate,
                endDate);

        List<UserDeliveredOrderCountDTO> deliveredList = orderRepository.countDeliveredOrdersByDate(userId, startDate,
                endDate);

        Map<LocalDate, UserOrderTimelineDTO> map = new TreeMap<>();

        if (createdList != null) {
            for (UserCreatedOrderCountDTO c : createdList) {
                LocalDate date = c.getDate().toLocalDate();
                map.put(
                        date,
                        new UserOrderTimelineDTO(date, c.getCreatedCount(), 0L));
            }
        }

        if (deliveredList != null) {
            for (UserDeliveredOrderCountDTO d : deliveredList) {
                if (d.getDate() != null) {
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
            }
        }

        return new ArrayList<>(map.values());
    }
}