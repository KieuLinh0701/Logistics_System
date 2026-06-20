package com.logistics.request.common.office;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PublicOfficeSearchRequest {
    private Integer city;
    private Integer ward;
    private Integer cityCode;
    private Integer wardCode;
    private String search;
}