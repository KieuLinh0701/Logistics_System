package com.logistics.request.user.order;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserOrderSearchRequest {
    private Integer page;
    private Integer limit;
    private String search;
    private String payer;
    private String status;
    private String pickupType;
    private Integer serviceTypeId;
    private String paymentStatus;
    private String cod;
    private String sort;
    private String startDate;
    private String endDate;
}
