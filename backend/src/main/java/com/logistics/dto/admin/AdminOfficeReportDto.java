package com.logistics.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOfficeReportDto {
    private Integer officeId;
    private String officeName;
    private Long totalOrders;
}
