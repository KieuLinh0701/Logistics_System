package com.logistics.request.shipper;

import lombok.Data;

import java.util.List;

@Data
public class SubmitCODRequest {
    private List<Integer> transactionIds; // Danh sách các transactionIds cần nôp
    private String notes; // Ghi chú
    private Integer totalAmount; // Thực nôpk
}
