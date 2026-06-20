package com.logistics.request.user.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserProductSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String type;
    private String status;
    private String stock;
    private String sort;
    private String startDate;
    private String endDate;
}
