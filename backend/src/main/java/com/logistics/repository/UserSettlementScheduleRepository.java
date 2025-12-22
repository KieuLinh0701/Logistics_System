package com.logistics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.logistics.entity.UserSettlementSchedule;
import com.logistics.enums.WeekDay;

@Repository
public interface UserSettlementScheduleRepository extends JpaRepository<UserSettlementSchedule, Integer> {
    UserSettlementSchedule findByUserId(Integer userId);

    @Query("SELECT s FROM UserSettlementSchedule s JOIN s.weekdays w WHERE w = :weekDay")
    List<UserSettlementSchedule> findAllWithScheduleToday(WeekDay weekDay);
}