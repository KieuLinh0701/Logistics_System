package com.logistics.request.manager.employee;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerEmployeeSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String sort;
    private String status;
    private String role;
    private String shift;
    private String startDate;
    private String endDate;
}
