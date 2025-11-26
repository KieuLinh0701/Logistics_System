package com.logistics.request.admin;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateFeeConfigurationRequest {
    private Integer serviceTypeId;
    private String feeType; // COD, PACKAGING, INSURANCE, VAT
    private String calculationType; // FIXED or PERCENTAGE
    private BigDecimal feeValue;
    private BigDecimal minOrderFee;
    private BigDecimal maxOrderFee;
    private Boolean active;
    private String notes;
}


