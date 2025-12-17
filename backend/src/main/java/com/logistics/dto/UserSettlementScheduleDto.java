package com.logistics.dto;

import java.util.Set;
import com.logistics.enums.WeekDay;
import lombok.Data;

@Data
public class UserSettlementScheduleDto {
    private Integer id;
    private Set<WeekDay> weekdays;
}