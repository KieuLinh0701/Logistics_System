package com.logistics.repository;

import com.logistics.dto.user.dashboard.UserTopProductItemDto;
import com.logistics.entity.OrderProduct;
import com.logistics.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderProductRepository
        extends JpaRepository<OrderProduct, Integer>, JpaSpecificationExecutor<OrderProduct> {
    List<OrderProduct> findByOrderId(Integer orderId);
        // Lấy danh sách OrderProduct theo orderId
        @Query("SELECT op FROM OrderProduct op JOIN FETCH op.product p WHERE op.order.id = :orderId")
        List<OrderProduct> findByOrderIdWithProduct(@Param("orderId") Integer orderId);

    // Top 5 sản phẩm bán chạy nhất
    @Query("""
                SELECT new com.logistics.dto.user.dashboard.UserTopProductItemDto(
                    p.id,
                    p.name,
                    SUM(op.quantity)
                )
                FROM OrderProduct op
                JOIN op.product p
                JOIN op.order o
                WHERE o.user.id = :userId
                  AND o.status = :status
                  AND o.createdAt BETWEEN :from AND :to
                GROUP BY p.id, p.name
                ORDER BY SUM(op.quantity) DESC
            """)
    List<UserTopProductItemDto> findTopSellingProducts(
            @Param("userId") Integer userId,
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    // Top 5 sản phẩm hoàn nhiều nhất
    @Query("""
                SELECT new com.logistics.dto.user.dashboard.UserTopProductItemDto(
                    p.id,
                    p.name,
                    COUNT(DISTINCT o.id)
                )
                FROM OrderProduct op
                JOIN op.product p
                JOIN op.order o
                WHERE o.user.id = :userId
                  AND o.status IN :returnStatuses
                  AND o.createdAt BETWEEN :from AND :to
                GROUP BY p.id, p.name
                ORDER BY COUNT(DISTINCT o.id) DESC
            """)
    List<UserTopProductItemDto> findTopReturnedProducts(
            @Param("userId") Integer userId,
            @Param("returnStatuses") List<OrderStatus> returnStatuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
    // Cập nhật số lượng đã giao  
    @Modifying
    @Query("""
            UPDATE OrderProduct p
            SET p.deliveredQuantity = COALESCE(p.deliveredQuantity, 0) + :qty
            WHERE p.id = :id
              AND (p.quantity - COALESCE(p.deliveredQuantity, 0) - COALESCE(p.returnedQuantity, 0)) >= :qty
            """)
    int incrementDelivered(@Param("id") Integer id, @Param("qty") int qty);
    
    // Cập nhật số lượng đã trả hàng
    @Modifying
    @Query("""
            UPDATE OrderProduct p
            SET p.returnedQuantity = COALESCE(p.returnedQuantity, 0) + :qty
            WHERE p.id = :id
              AND (p.quantity - COALESCE(p.deliveredQuantity, 0) - COALESCE(p.returnedQuantity, 0)) >= :qty
            """)
    int incrementReturned(@Param("id") Integer id, @Param("qty") int qty);

}