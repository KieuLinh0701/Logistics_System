package com.logistics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.logistics.entity.UserSettlementSchedule;

@Repository
public interface UserSettlementScheduleRepository extends JpaRepository<UserSettlementSchedule, Integer> {
    UserSettlementSchedule findByUserId(Integer userId);
}