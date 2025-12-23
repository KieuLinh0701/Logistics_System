package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.logistics.entity.OrderHistory;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Integer>, JpaSpecificationExecutor<OrderHistory> {
    List<OrderHistory> findByOrderId(Integer orderId);

    List<OrderHistory> findByOrderIdOrderByActionTimeDesc(Integer orderId);
}
