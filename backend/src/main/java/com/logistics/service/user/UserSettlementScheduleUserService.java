package com.logistics.service.user;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import com.logistics.exception.AppException;
import com.logistics.exception.enums.UserErrorCode;
import com.logistics.exception.enums.UserSettlementScheduleErrorCode;
import org.springframework.stereotype.Service;

import com.logistics.dto.UserSettlementScheduleDto;
import com.logistics.entity.User;
import com.logistics.entity.UserSettlementSchedule;
import com.logistics.enums.WeekDay;
import com.logistics.repository.UserRepository;
import com.logistics.repository.UserSettlementScheduleRepository;
import com.logistics.response.ApiResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSettlementScheduleUserService {

    private final UserSettlementScheduleRepository scheduleRepository;
    private final UserUserService userService;

    public UserSettlementScheduleDto getUserSchedule(Integer userId) {
            Integer shopId = userService.getShopId(userId);

            UserSettlementSchedule schedule = scheduleRepository.findByUserId(shopId);

            if (schedule == null) {
                throw  new AppException(UserSettlementScheduleErrorCode.SETTLEMENT_SCHEDULE_NOT_FOUND);
            }

            UserSettlementScheduleDto dto = new UserSettlementScheduleDto();
            dto.setWeekdays(schedule.getWeekdays());

            return dto;
    }

    @Transactional
    public void updateUserSchedule(Integer userId, Set<String> weekdays) {
            Integer shopId = userService.getShopId(userId);
            User user = userService.getUser(shopId);

            if (weekdays == null || weekdays.isEmpty()) {
                throw new AppException(UserSettlementScheduleErrorCode.SETTLEMENT_SCHEDULE_INVALID_DAY_COUNT);
            }

            Set<WeekDay> weekDaysEnum = new HashSet<>();
            for (String day : weekdays) {
                try {
                    weekDaysEnum.add(WeekDay.valueOf(day));
                } catch (IllegalArgumentException e) {
                    throw new AppException(UserSettlementScheduleErrorCode.SETTLEMENT_SCHEDULE_INVALID_DAY_FORMAT);
                }
            }

            UserSettlementSchedule schedule = scheduleRepository.findByUserId(shopId);
            if (schedule == null) {
                schedule = new UserSettlementSchedule();
                schedule.setUser(user);
            }

            schedule.setWeekdays(weekDaysEnum);
            scheduleRepository.save(schedule);
    }

    public String getNextSettlementDate(Integer userId) {
        UserSettlementSchedule schedule = scheduleRepository.findByUserId(userId);
        if (schedule == null || schedule.getWeekdays().isEmpty()) {
            return null;
        }

        Set<WeekDay> weekdays = schedule.getWeekdays();
        LocalDate nextSettlement = LocalDate.now();

        for (int i = 0; i < 7; i++) {
            DayOfWeek dow = nextSettlement.getDayOfWeek();
            if (weekdays.contains(WeekDay.valueOf(dow.name()))) {
                break; 
            }
            nextSettlement = nextSettlement.plusDays(1);
        }

        return nextSettlement.atStartOfDay()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}