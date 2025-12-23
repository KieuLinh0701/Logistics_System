package com.logistics.dto.user.dashboard;

import java.time.LocalDate;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderTimelineDTO {
    private LocalDate date;
    private Long createdCount;
    private Long deliveredCount;
}
