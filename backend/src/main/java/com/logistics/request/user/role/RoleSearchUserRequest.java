package com.logistics.request.user.role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleSearchUserRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String startDate;
    private String endDate;
}
