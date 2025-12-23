package com.logistics.dto.user.dashboard;

import java.sql.Date;

import lombok.*;

@Getter
@AllArgsConstructor
public class UserDeliveredOrderCountDTO {
    private Date date; 
    private Long deliveredCount;
}
