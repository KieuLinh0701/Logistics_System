package com.logistics.request.admin;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdatePromotionRequest {
    private String code;
    private String title;
    private String description;
    private String discountType; // PERCENTAGE or FIXED
    private BigDecimal discountValue;
    private Boolean isGlobal;
    private Integer maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    // Điều kiện áp dụng
    private BigDecimal minOrderValue;
    private BigDecimal minWeight;
    private BigDecimal maxWeight;
    private Integer minOrdersCount;
    private List<Integer> serviceTypeIds;
    private Boolean firstTimeUser;
    private Integer validMonthsAfterJoin;
    private Integer validYearsAfterJoin;
    
    // Giới hạn sử dụng
    private Integer usageLimit;
    private Integer maxUsagePerUser;
    private Integer dailyUsageLimitGlobal;
    private Integer dailyUsageLimitPerUser;
    
    private String status; // ACTIVE, INACTIVE, EXPIRED
    
    // Danh sách user IDs để gán khuyến mãi (nếu isGlobal = false)
    private List<Integer> userIds;
}


