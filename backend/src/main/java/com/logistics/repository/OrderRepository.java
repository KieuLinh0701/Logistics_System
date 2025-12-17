package com.logistics.repository;

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

    List<Order> findByUserAndSettlementBatchIsNullAndStatusIn(User user, List<OrderStatus> statuses);


}
