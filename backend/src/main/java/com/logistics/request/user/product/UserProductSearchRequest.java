package com.logistics.request.user.product;

import lombok.*;

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
