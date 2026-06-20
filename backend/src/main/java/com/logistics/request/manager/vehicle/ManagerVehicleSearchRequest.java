package com.logistics.request.manager.vehicle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerVehicleSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String type;
    private String status;
    private String sort;
    private String startDate;
    private String endDate;
}
