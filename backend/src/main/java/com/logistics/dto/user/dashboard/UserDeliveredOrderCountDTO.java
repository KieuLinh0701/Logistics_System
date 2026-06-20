package com.logistics.dto.user.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Date;

@Getter
@AllArgsConstructor
public class UserDeliveredOrderCountDTO {
    private Date date; 
    private Long deliveredCount;
}
