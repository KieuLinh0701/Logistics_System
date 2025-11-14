package com.logistics.request.office;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OfficeSearchRequest {
    private Integer city;
    private Integer ward;
    private String search;
}