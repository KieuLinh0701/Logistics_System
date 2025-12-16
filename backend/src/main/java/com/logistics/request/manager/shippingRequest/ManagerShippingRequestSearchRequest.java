package com.logistics.request.manager.shippingRequest;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerShippingRequestSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String type;
    private String status;
    private String sort;
    private String startDate;
    private String endDate;
}
