package com.logistics.dto.user.dashboard;

import java.sql.Date;

import lombok.*;

@Getter
@AllArgsConstructor
public class UserCreatedOrderCountDTO {
    private Date date; 
    private Long createdCount;
}