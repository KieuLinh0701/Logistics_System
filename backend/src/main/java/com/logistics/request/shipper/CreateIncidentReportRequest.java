package com.logistics.request.shipper;

import lombok.Data;

@Data
public class CreateIncidentReportRequest {

    private Integer orderId;
    private String incidentType;
    private String title;
    private String description;
    private Integer addressId;
    private String priority;
}
