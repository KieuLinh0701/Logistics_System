package com.logistics.dto.user.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderTimelineDTO {
    private LocalDate date;
    private Long createdCount;
    private Long deliveredCount;
}
