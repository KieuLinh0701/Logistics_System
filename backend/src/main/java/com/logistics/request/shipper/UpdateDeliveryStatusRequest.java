package com.logistics.request.shipper;

import lombok.Data;

@Data
public class UpdateDeliveryStatusRequest {

    private String status;
    private String notes;
    private String actualRecipient;
    private String actualRecipientPhone;
}
