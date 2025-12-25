package com.logistics.request.shipper;

import lombok.Data;

@Data
public class PickedUpRequest {
    private Double latitude;
    private Double longitude;
    private String photoUrl;
    private String notes;
}
