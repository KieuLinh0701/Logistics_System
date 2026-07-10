package com.logistics.request.manager.attempt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttemptSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;

    private String category;

    private String status;

    private String sort;
    private String startDate;
    private String endDate;
}
