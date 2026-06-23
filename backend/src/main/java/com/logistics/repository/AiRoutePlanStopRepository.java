package com.logistics.repository;

import com.logistics.entity.AiRoutePlanStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AiRoutePlanStopRepository extends JpaRepository<AiRoutePlanStop, Long> {

    boolean existsByRouteIdAndOrderId(Long routeId, Integer orderId);

    @Query("SELECT MAX(s.stopSequence) FROM AiRoutePlanStop s WHERE s.route.id = :routeId")
    Optional<Integer> findMaxStopSequenceByRouteId(@Param("routeId") Long routeId);

    List<AiRoutePlanStop> findByRouteIdOrderByStopSequenceAsc(Long routeId);
}
