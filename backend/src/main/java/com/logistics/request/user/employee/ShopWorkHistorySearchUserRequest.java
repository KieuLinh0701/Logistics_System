package com.logistics.request.user.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShopWorkHistorySearchUserRequest {
    private int page = 1;
    private int limit = 10;
    private String search;
    private Boolean isCurrent;
    private String sort;
    private String startDate;
    private String endDate;
}
