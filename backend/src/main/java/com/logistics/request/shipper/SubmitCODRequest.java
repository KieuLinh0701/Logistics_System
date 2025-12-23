package com.logistics.request.shipper;

import lombok.Data;
import java.util.List;

@Data
public class SubmitCODRequest {
    private List<Integer> transactionIds; // Danh sách ID transaction COD cần nộp
    private Integer totalAmount; // Tổng số tiền nộp
    private String notes; // Ghi chú
    private List<String> imageUrls; // Ảnh biên lai
}
