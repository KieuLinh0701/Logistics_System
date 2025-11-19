package com.logistics.request.admin;

import lombok.Data;

@Data
public class UpdateServiceTypeRequest {
    private String name;
    private String description;
    private String status;
    private String deliveryTime;
    private Integer deliveryTimeFrom;
    private Integer deliveryTimeTo;
    private String deliveryTimeUnit;
}



