package com.logistics.request.user.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserUrgentOrderSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String startDate;
    private String endDate;
    private String sort;
}
