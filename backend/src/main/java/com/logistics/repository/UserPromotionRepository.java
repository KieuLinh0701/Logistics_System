package com.logistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logistics.entity.UserPromotion;

@Repository
public interface UserPromotionRepository extends JpaRepository<UserPromotion, Integer> {

    UserPromotion findByUserIdAndPromotionId(Integer userId, Integer promotionId);

}