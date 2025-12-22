package com.logistics.repository;

import com.logistics.dto.user.dashboard.UserCreatedOrderCountDTO;
import com.logistics.dto.user.dashboard.UserDeliveredOrderCountDTO;
import com.logistics.dto.user.dashboard.UserOrderStatsDTO;
import com.logistics.dto.user.dashboard.UserOrderTimelineDTO;
import com.logistics.entity.Order;
import com.logistics.entity.User;
import com.logistics.enums.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {
    Optional<Order> findByTrackingNumber(String trackingNumber);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUserId(Integer userId);

    @Query("SELECT o FROM Order o WHERE o.trackingNumber LIKE %:search% OR o.senderName LIKE %:search% OR o.recipientName LIKE %:search%")
    List<Order> searchOrders(@Param("search") String search);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByUserIdAndStatus(Integer userId, OrderStatus status);

    boolean existsByTrackingNumber(String trackingNumber);

    long countByUserIdAndStatusNot(Integer userId, OrderStatus status);

    Optional<Order> findByTrackingNumberAndUserId(String trackingNumber, int userId);

    Optional<Order> findByIdAndUserId(Integer id, int userId);

    List<Order> findByUserIdAndIdIn(Integer userId, List<Integer> orderIds);

    List<Order> findByIdIn(List<Integer> orderIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Integer id);
    List<Order> findByUserAndSettlementBatchIsNullAndStatusIn(User user, List<OrderStatus> statuses);

    // Dashboard của user
    @Query("SELECT new com.logistics.dto.user.dashboard.UserOrderStatsDTO(" +
            "COUNT(o), " +
            "SUM(CASE WHEN o.status = 'DRAFT' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = 'PENDING' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = 'CONFIRMED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = 'READY_FOR_PICKUP' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = 'PICKING_UP' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status IN ('PICKED_UP','AT_ORIGIN_OFFICE','IN_TRANSIT','AT_DEST_OFFICE') THEN 1 ELSE 0 END), "
            +
            "SUM(CASE WHEN o.status = 'DELIVERING' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = 'FAILED_DELIVERY' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status = 'RETURNING' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.status IN ('RETURNED', 'CANCELLED') THEN 1 ELSE 0 END)) " +
            "FROM Order o WHERE o.user.id = :userId")
    UserOrderStatsDTO getUserOrderStats(@Param("userId") Integer userId);

    // Tính số tiền người dùng có thể sẽ nhận trong phiên đối soát sắp tới (tính tới
    // thời điểm hiện tại)
    @Query("""
                SELECT COALESCE(SUM(
                    CASE
                        WHEN o.status = com.logistics.enums.OrderStatus.RETURNED
                             AND o.paymentStatus = com.logistics.enums.OrderPaymentStatus.UNPAID
                            THEN -o.totalFee
                        WHEN o.status = com.logistics.enums.OrderStatus.RETURNED
                             AND o.paymentStatus = com.logistics.enums.OrderPaymentStatus.PAID
                             AND o.payer = com.logistics.enums.OrderPayerType.CUSTOMER
                            THEN 0
                        WHEN o.status = com.logistics.enums.OrderStatus.DELIVERED
                             AND o.paymentStatus = com.logistics.enums.OrderPaymentStatus.PAID
                             AND o.payer = com.logistics.enums.OrderPayerType.CUSTOMER
                            THEN o.cod
                        ELSE o.cod - o.totalFee
                    END
                ), 0)
                FROM Order o
                WHERE o.user.id = :userId
                  AND o.status IN :statuses
                  AND o.settlementBatch IS NULL
            """)
    BigDecimal sumPendingCODNow(Integer userId, List<OrderStatus> statuses);

    // Số đơn tạo thành công theo ngày tạo
    @Query("""
                SELECT new com.logistics.dto.user.dashboard.UserCreatedOrderCountDTO(
                    cast(o.createdAt as date),
                    COUNT(o.id)
                )
                FROM Order o
                WHERE o.user.id = :userId
                  AND (:startDate IS NULL OR o.createdAt >= :startDate)
                  AND (:endDate IS NULL OR o.createdAt <= :endDate)
                GROUP BY cast(o.createdAt as date)
                ORDER BY cast(o.createdAt as date)
            """)
    List<UserCreatedOrderCountDTO> countCreatedOrdersByDate(
            Integer userId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    // Số đơn giao thành công theo ngày giao
    @Query("""
                SELECT new com.logistics.dto.user.dashboard.UserDeliveredOrderCountDTO(
                    cast(o.deliveredAt as date),
                    COUNT(o.id)
                )
                FROM Order o
                WHERE o.user.id = :userId
                  AND o.status = com.logistics.enums.OrderStatus.DELIVERED
                  AND (:startDate IS NULL OR o.deliveredAt >= :startDate)
                  AND (:endDate IS NULL OR o.deliveredAt <= :endDate)
                GROUP BY cast(o.deliveredAt as date)
                ORDER BY cast(o.deliveredAt as date)
            """)
    List<UserDeliveredOrderCountDTO> countDeliveredOrdersByDate(
            Integer userId,
            LocalDateTime startDate,
            LocalDateTime endDate);

}
