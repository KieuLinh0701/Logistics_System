package com.logistics.dto.user.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShopWorkHistoryListUserDto {
    private Integer id;
    private String roleName;
    private Boolean isCurrent;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private String note;
}
