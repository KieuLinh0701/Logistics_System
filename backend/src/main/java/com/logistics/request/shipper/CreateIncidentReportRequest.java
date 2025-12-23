package com.logistics.request.shipper;

import lombok.Data;
import java.util.List;

@Data
public class CreateIncidentReportRequest {

    private Integer orderId;
    private String incidentType;
    private String title;
    private String description;
    private Integer addressId;
    private String priority;
    private List<String> images;
}
