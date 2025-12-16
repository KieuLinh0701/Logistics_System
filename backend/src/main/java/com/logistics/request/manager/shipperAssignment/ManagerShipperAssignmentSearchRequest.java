package com.logistics.request.manager.shipperAssignment;

import lombok.*;

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
