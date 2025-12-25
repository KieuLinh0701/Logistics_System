package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ManagerEmployeeStatsDTO {
    private Long total = 0L;
    private Long active = 0L; 
    private Long inactive = 0L;
    private Long leave = 0L;

    public ManagerEmployeeStatsDTO(Long total, Long active, Long inactive, Long leave) {
        this.total = total;
        this.active = active;
        this.inactive = inactive;
        this.leave = leave;
    }
}