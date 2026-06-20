package com.logistics.repository;

import com.logistics.entity.UserSettlementSchedule;
import com.logistics.enums.WeekDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSettlementScheduleRepository extends JpaRepository<UserSettlementSchedule, Integer> {
    UserSettlementSchedule findByUserId(Integer userId);

    @Query("SELECT s FROM UserSettlementSchedule s JOIN s.weekdays w WHERE w = :weekDay")
    List<UserSettlementSchedule> findAllWithScheduleToday(WeekDay weekDay);
}