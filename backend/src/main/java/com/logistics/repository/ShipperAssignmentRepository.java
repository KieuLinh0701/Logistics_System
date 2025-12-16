package com.logistics.repository;

import com.logistics.entity.ShipperAssignment;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipperAssignmentRepository
        extends JpaRepository<ShipperAssignment, Long>,
                JpaSpecificationExecutor<ShipperAssignment> {

    /**
     * Check if there is any overlapping assignment for a shipper in a given area.
     * - For create: excludeId = null
     * - For update: excludeId = current assignment's id
     */
    @Query("""
        SELECT CASE WHEN COUNT(sa) > 0 THEN true ELSE false END
        FROM ShipperAssignment sa
        WHERE sa.shipper.id = :shipperId
          AND sa.cityCode = :cityCode
          AND sa.wardCode = :wardCode
          AND (sa.endAt IS NULL OR sa.endAt >= :startAt)
          AND (:endAt IS NULL OR sa.startAt <= :endAt)
          AND (:excludeId IS NULL OR sa.id <> :excludeId)
    """)
    boolean existsActiveOverlap(
            @Param("shipperId") Integer shipperId,
            @Param("cityCode") Integer cityCode,
            @Param("wardCode") Integer wardCode,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludeId") Long excludeId
    );

}