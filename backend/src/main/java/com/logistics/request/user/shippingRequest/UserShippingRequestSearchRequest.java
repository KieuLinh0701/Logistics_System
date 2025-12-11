package com.logistics.request.user.shippingRequest;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserShippingRequestSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String type;
    private String status;
    private String sort;
    private String startDate;
    private String endDate;
}
