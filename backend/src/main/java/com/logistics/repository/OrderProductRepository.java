package com.logistics.repository;

import com.logistics.dto.user.dashboard.UserTopProductItemDto;
import com.logistics.entity.OrderProduct;
import com.logistics.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
                op.product.id,
                op.product.name,
                SUM(op.quantity)
            )
            FROM OrderProduct op
            JOIN op.order o
            WHERE o.user.id = :userId
              AND o.status IN :statuses
              AND o.createdAt BETWEEN COALESCE(:from, CAST('1970-01-01T00:00:00' AS localdatetime)) 
              AND COALESCE(:to, CAST('2099-12-31T23:59:59' AS localdatetime))
            GROUP BY op.product.id, op.product.name
            ORDER BY SUM(op.quantity) DESC
            LIMIT 5
        """)
    List<UserTopProductItemDto> findTopSellingProducts(
            @Param("userId") Integer userId,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Top 5 sản phẩm hoàn nhiều nhất
    @Query("""
            SELECT new com.logistics.dto.user.dashboard.UserTopProductItemDto(
                op.product.id,
                op.product.name,
                SUM(op.returnedQuantity) 
            )
            FROM OrderProduct op
            JOIN op.order o
            WHERE o.user.id = :userId
              AND o.status IN :returnStatuses
              AND o.createdAt BETWEEN COALESCE(:from, CAST('1970-01-01T00:00:00' AS localdatetime)) 
              AND COALESCE(:to, CAST('2099-12-31T23:59:59' AS localdatetime))
            GROUP BY op.product.id, op.product.name
            ORDER BY SUM(op.returnedQuantity) DESC
            LIMIT 5
        """)
    List<UserTopProductItemDto> findTopReturnedProducts(
            @Param("userId") Integer userId,
            @Param("returnStatuses") List<OrderStatus> returnStatuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

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