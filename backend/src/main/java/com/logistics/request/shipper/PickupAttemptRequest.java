package com.logistics.request.shipper;

import lombok.Data;

@Data
public class PickupAttemptRequest {
    private String status;
    private String failReason;
    private String note;
}
