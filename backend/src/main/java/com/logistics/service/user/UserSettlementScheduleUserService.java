package com.logistics.service.user;

import com.logistics.dto.UserSettlementScheduleDto;

import java.util.Set;

public interface UserSettlementScheduleUserService {
    UserSettlementScheduleDto getUserSchedule(Integer userId);
    void updateUserSchedule(Integer userId, Set<String> weekdays);
    String getNextSettlementDate(Integer userId);
}