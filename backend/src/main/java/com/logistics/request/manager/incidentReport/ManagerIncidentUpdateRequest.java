package com.logistics.request.manager.incidentReport;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManagerIncidentUpdateRequest { 
    private String status;
    private String resolution;
}
