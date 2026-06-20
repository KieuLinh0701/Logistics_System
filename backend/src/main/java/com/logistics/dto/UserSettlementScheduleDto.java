package com.logistics.dto;

import com.logistics.enums.WeekDay;
import lombok.Data;

import java.util.Set;

@Data
public class UserSettlementScheduleDto {
    private Integer id;
    private Set<WeekDay> weekdays;
}