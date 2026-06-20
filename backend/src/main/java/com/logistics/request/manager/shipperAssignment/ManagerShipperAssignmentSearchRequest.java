package com.logistics.request.manager.shipperAssignment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShipperAssignmentSearchRequest {
    private Integer page;
    private Integer limit;
    private String sort;
    private String search;
    private Integer wardCode;
    private String startDate;
    private String endDate;
}
