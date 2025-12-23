package com.logistics.dto.manager.dashboard;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerEmployeeStatsDTO {
    private long total;
    private long active; 
    private long inactive;
    private long leave;
}
