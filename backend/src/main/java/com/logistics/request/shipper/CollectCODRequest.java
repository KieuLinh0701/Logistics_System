package com.logistics.request.shipper;

import lombok.Data;

@Data
public class CollectCODRequest {
    private Integer orderId; // ID đơn hàng
    private Integer actualAmount; // Số tiền thực thu 
    private String notes; // Ghi chú
}
