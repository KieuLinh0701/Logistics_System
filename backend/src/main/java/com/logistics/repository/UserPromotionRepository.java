package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.logistics.entity.UserPromotion;

@Repository
public interface UserPromotionRepository extends JpaRepository<UserPromotion, Integer> {

    @Query("""
        SELECT up FROM UserPromotion up
        WHERE up.promotion.id = :promotionId
          AND (up.user.id = :userId OR up.user IS NULL)
    """)
    List<UserPromotion> findApplicableUserPromotion(Integer promotionId, Integer userId);
}