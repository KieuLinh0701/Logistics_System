package com.logistics.request;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String sort;
    private String status;
    private String type;
    private String role;
    private String shift;
    private String startDate;
    private String endDate;
}
