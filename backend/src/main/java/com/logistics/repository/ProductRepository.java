package com.logistics.repository;

import com.logistics.dto.user.dashboard.UserProductStatsDTO;
import com.logistics.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {
    List<Product> findByUserId(Integer userId);

    boolean existsByUserIdAndName(Integer userId, String name);

    // Tổng quan sản phẩm
    @Query("SELECT new com.logistics.dto.user.dashboard.UserProductStatsDTO(" +
            "COUNT(p), " +
            "COALESCE(SUM(CASE WHEN p.stock = 0 THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN p.stock < 10 THEN 1 ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN p.status = 'ACTIVE' THEN 1 ELSE 0 END), 0)) " +
            "FROM Product p WHERE p.user.id = :userId")
    UserProductStatsDTO getUserProductStats(@Param("userId") Integer userId);

    // Lấy số lượng sản phẩm theo từng loại của 1 user
    @Query("SELECT p.type, COUNT(p) FROM Product p WHERE p.user.id = :userId GROUP BY p.type")
    List<Object[]> countProductsByTypeForUser(@Param("userId") Integer userId);

}