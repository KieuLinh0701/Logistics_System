package com.logistics.request.user.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeSearchUserRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String sort;
    private Integer roleId;
    private Boolean active;
    private String startDate;
    private String endDate;
}