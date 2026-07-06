package com.logistics.request.shipper;

import lombok.Data;

@Data
public class UpdateDeliveryStatusRequest {

    private String status;
    private String notes;
    private String failReason;
    private String actualRecipient;
    private String actualRecipientPhone;
    private String proofImageUrl;

    private String collectionMode;
    private Integer actualCollected;
    private String collectionNote;
}
