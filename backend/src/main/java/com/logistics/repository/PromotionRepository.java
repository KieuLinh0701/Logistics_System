package com.logistics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.logistics.entity.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer>, JpaSpecificationExecutor<Promotion> {
    Optional<Promotion> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.promotion.id = :promotionId AND DATE(o.createdAt) = CURRENT_DATE")
    int countTodayUsageGlobal(Integer promotionId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.promotion.id = :promotionId AND o.user.id = :userId AND DATE(o.createdAt) = CURRENT_DATE")
    int countTodayUsageByUser(Integer userId, Integer promotionId);

    

}