package com.logistics.repository;

import com.logistics.entity.Order;
import com.logistics.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByTrackingNumber(String trackingNumber);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByUserId(Integer userId);
    
    @Query("SELECT o FROM Order o WHERE o.trackingNumber LIKE %:search% OR o.senderName LIKE %:search% OR o.recipientName LIKE %:search%")
    List<Order> searchOrders(@Param("search") String search);
    
    org.springframework.data.domain.Page<Order> findByStatus(OrderStatus status, org.springframework.data.domain.Pageable pageable);
}

