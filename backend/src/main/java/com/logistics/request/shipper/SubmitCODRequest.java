package com.logistics.request.shipper;

import lombok.Data;

@Data
public class SubmitCODRequest {
    private Integer batchId; // ID cua batch can nop
    private String notes; // Ghi chú
}
