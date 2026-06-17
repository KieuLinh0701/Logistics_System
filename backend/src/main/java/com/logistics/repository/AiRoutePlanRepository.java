package com.logistics.repository;

import com.logistics.entity.AiRoutePlan;
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
            LEFT JOIN FETCH p.routes
            WHERE p.id = :id AND p.office.id = :officeId
            """)
    Optional<AiRoutePlan> findByIdAndOfficeIdWithDetails(@Param("id") Long id, @Param("officeId") Integer officeId);

    Optional<AiRoutePlan> findFirstByOfficeIdAndStatusOrderByConfirmedAtDesc(
            Integer officeId,
            AiRoutePlanStatus status);
}
