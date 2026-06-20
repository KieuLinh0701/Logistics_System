package com.logistics.request.manager.incidentReport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManagerIncidentUpdateRequest { 
    private String status;
    private String resolution;
}
