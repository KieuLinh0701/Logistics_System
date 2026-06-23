package com.logistics.repository;

import com.logistics.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
   Optional<User> findByAccountId(Integer accountId);

   boolean existsByPhoneNumber(String phoneNumber);

   @Query("SELECT u FROM User u " +
         "JOIN FETCH u.account a " +
         "JOIN FETCH a.accountRoles ar " +
         "LEFT JOIN FETCH ar.role " +
         "WHERE u.id = :userId")
   Optional<User> findByIdWithRoles(@Param("userId") Integer userId);

    List<User> findByLockedTrue();

    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN ShopWorkHistory swh ON swh.user = u " +
            "JOIN u.account a " +
            "WHERE swh.shop.id = :shopId " +
            "AND (:roleId IS NULL OR swh.role.id = :roleId) " +
            "AND swh.id = (" +
            "   SELECT MAX(swh2.id) FROM ShopWorkHistory swh2 " +
            "   WHERE swh2.user = u AND swh2.shop.id = :shopId" +
            ") " +
            "AND (:active IS NULL OR swh.isCurrent = :active) " +
            "AND (:search IS NULL OR " +
            "CONCAT(u.lastName, ' ', u.firstName) LIKE %:search% OR " +
            "u.code LIKE %:search% OR " +
            "a.email LIKE %:search% OR " +
            "u.phoneNumber LIKE %:search%) " +
            "AND (:startDate IS NULL OR u.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR u.createdAt <= :endDate)")
    Page<User> findAllByShopIdWithLatestWorkHistory(
            @Param("shopId") Integer shopId,
            @Param("roleId") Integer roleId,
            @Param("search") String search,
            @Param("active") Boolean active,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Stage 1a: shipper trùng wardCode (ưu tiên cao)
    @Query("""
        SELECT DISTINCT sa.shipper FROM ShipperAssignment sa
        JOIN sa.shipper.account acc
        JOIN acc.accountRoles ar
        JOIN ar.role r
        WHERE sa.cityCode = :cityCode
          AND sa.wardCode = :wardCode
          AND sa.startAt <= :now
          AND (sa.endAt IS NULL OR sa.endAt > :now)
          AND acc.isActive = true
          AND ar.isActive = true
          AND r.name = 'Shipper'
          AND r.userOwner IS NULL
    """)
    List<User> findActiveShippersByWard(
            @Param("cityCode") Integer cityCode,
            @Param("wardCode") Integer wardCode,
            @Param("now") LocalDateTime now
    );

    // Stage 1b + Stage 2: toàn bộ shipper trong city
    @Query("""
        SELECT DISTINCT sa.shipper FROM ShipperAssignment sa
        JOIN sa.shipper.account acc
        JOIN acc.accountRoles ar
        JOIN ar.role r
        WHERE sa.cityCode = :cityCode
          AND sa.startAt <= :now
          AND (sa.endAt IS NULL OR sa.endAt > :now)
          AND acc.isActive = true
          AND ar.isActive = true
          AND r.name = 'Shipper'
          AND r.userOwner IS NULL
    """)
    List<User> findActiveShippersByCity(
            @Param("cityCode") Integer cityCode,
            @Param("now") LocalDateTime now
    );
}