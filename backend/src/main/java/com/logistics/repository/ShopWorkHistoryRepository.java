package com.logistics.repository;

import com.logistics.entity.Role;
import com.logistics.entity.ShopWorkHistory;
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
public interface ShopWorkHistoryRepository
        extends JpaRepository<ShopWorkHistory, Integer> {

    List<ShopWorkHistory> findByUserIdAndIsCurrentTrue(Integer userId);

    Optional<ShopWorkHistory> findByUserIdAndRoleIdAndIsCurrentTrue(Integer userId, Integer roleId);

    @Query("""
            SELECT swh FROM ShopWorkHistory swh
            LEFT JOIN swh.role r
            LEFT JOIN swh.shop s
            WHERE swh.user.id = :userId
            AND (:isCurrent IS NULL OR swh.isCurrent = :isCurrent)
            AND (
                :search IS NULL OR :search = ''
                OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(swh.note) LIKE LOWER(CONCAT('%', :search, '%'))
            )
            AND (:startDate IS NULL OR swh.joinedAt >= :startDate)
            AND (:endDate IS NULL OR swh.joinedAt <= :endDate)
            """)
    Page<ShopWorkHistory> findAllByUserIdWithFilter(
            @Param("userId") int userId,
            @Param("isCurrent") Boolean isCurrent,
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}