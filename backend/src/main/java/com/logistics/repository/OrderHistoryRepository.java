package com.logistics.repository;

import com.logistics.entity.OrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Integer>, JpaSpecificationExecutor<OrderHistory> {
    List<OrderHistory> findByOrderId(Integer orderId);

    List<OrderHistory> findByOrderIdOrderByActionTimeDesc(Integer orderId);
}
