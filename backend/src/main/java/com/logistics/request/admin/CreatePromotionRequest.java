package com.logistics.request.admin;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreatePromotionRequest {
    private String code;
    private String title;
    private String description;
    private String discountType; // PERCENTAGE or FIXED
    private BigDecimal discountValue;
    private Boolean isGlobal = true; // true = khuyến mãi chung, false = khuyến mãi theo user
    private Integer maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Điều kiện áp dụng
    private BigDecimal minOrderValue;
    private BigDecimal minWeight;
    private BigDecimal maxWeight;
    private Integer minOrdersCount; // Số đơn tối thiểu khách đã tạo
    private List<Integer> serviceTypeIds; // Danh sách service type IDs
    private Boolean firstTimeUser = false;
    private Integer validMonthsAfterJoin;
    private Integer validYearsAfterJoin;
    
    // Giới hạn sử dụng
    private Integer usageLimit; // Tổng số lần sử dụng
    private Integer maxUsagePerUser; // Số lần tối đa mỗi user
    private Integer dailyUsageLimitGlobal; // Giới hạn theo ngày (global)
    private Integer dailyUsageLimitPerUser; // Giới hạn theo ngày (mỗi user)
    
    private String status; // ACTIVE, INACTIVE, EXPIRED
    
    // Danh sách user IDs để gán khuyến mãi (nếu isGlobal = false)
    private List<Integer> userIds;
}


