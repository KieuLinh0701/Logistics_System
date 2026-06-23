package com.logistics.repository;

import com.logistics.entity.AiRoutePlan;
import com.logistics.entity.AiRoutePlanRoute;
import com.logistics.entity.AiRoutePlanStop;
import com.logistics.enums.AiRoutePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiRoutePlanRepository extends JpaRepository<AiRoutePlan, Long> {

    List<AiRoutePlan> findByOfficeIdAndStatusOrderByCreatedAtDesc(Integer officeId, AiRoutePlanStatus status);

    @Query("""
            SELECT p FROM AiRoutePlan p
            LEFT JOIN FETCH p.office
            LEFT JOIN FETCH p.managerEmployee
            WHERE p.id = :id AND p.office.id = :officeId
            """)
    Optional<AiRoutePlan> findByIdAndOfficeIdWithDetails(@Param("id") Long id, @Param("officeId") Integer officeId);

    @Query("""
            SELECT r FROM AiRoutePlanRoute r
            LEFT JOIN FETCH r.plan
            WHERE r.plan.id = :planId
            ORDER BY r.routeSequence ASC
            """)
    List<AiRoutePlanRoute> findRoutesByPlanId(@Param("planId") Long planId);

    @Query("""
            SELECT s FROM AiRoutePlanStop s
            LEFT JOIN FETCH s.order
            WHERE s.route.id IN :routeIds
            ORDER BY s.stopSequence ASC
            """)
    List<AiRoutePlanStop> findStopsByRouteIds(@Param("routeIds") List<Long> routeIds);

    Optional<AiRoutePlan> findFirstByOfficeIdAndStatusOrderByConfirmedAtDesc(
            Integer officeId,
            AiRoutePlanStatus status);
}
