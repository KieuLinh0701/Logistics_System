package com.logistics.request.shipper;

import lombok.Data;

@Data
public class DeliverOriginRequest {
    private Integer officeId;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String notes;
}
