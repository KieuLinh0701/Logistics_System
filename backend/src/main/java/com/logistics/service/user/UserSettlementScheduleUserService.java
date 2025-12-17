package com.logistics.service.user;

import java.util.HashSet;
import java.util.Set;

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
    private final UserRepository userRepository;

    public ApiResponse<UserSettlementScheduleDto> getUserSchedule(Integer userId) {
        try {
            UserSettlementSchedule schedule = scheduleRepository.findByUserId(userId);

            if (schedule == null) {
                return new ApiResponse<>(true, "Người dùng chưa có lịch đối soát", null);
            }

            UserSettlementScheduleDto dto = new UserSettlementScheduleDto();
            dto.setWeekdays(schedule.getWeekdays());

            return new ApiResponse<>(true, "Lấy lịch thành công", dto);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> updateUserSchedule(Integer userId, Set<String> weekdays) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return new ApiResponse<>(false, "Người dùng không tồn tại", false);
            }

            if (weekdays == null || weekdays.isEmpty()) {
                return new ApiResponse<>(false, "Vui lòng chọn ít nhất 1 ngày", false);
            }

            Set<WeekDay> weekDaysEnum = new HashSet<>();
            for (String day : weekdays) {
                try {
                    weekDaysEnum.add(WeekDay.valueOf(day));
                } catch (IllegalArgumentException e) {
                    return new ApiResponse<>(false, "Ngày không hợp lệ: " + day, false);
                }
            }

            UserSettlementSchedule schedule = scheduleRepository.findByUserId(userId);
            if (schedule == null) {
                schedule = new UserSettlementSchedule();
                schedule.setUser(user);
            }

            schedule.setWeekdays(weekDaysEnum);
            scheduleRepository.save(schedule);

            return new ApiResponse<>(true, "Cập nhật lịch thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi cập nhật lịch: " + e.getMessage(), false);
        }
    }
}