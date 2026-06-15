package com.logistics.request.user.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeByRoleIdSearchUserRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String startDate;
    private String endDate;
}
