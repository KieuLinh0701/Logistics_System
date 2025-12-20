package com.logistics.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @deprecated Use {@link com.logistics.dto.admin.AdminOfficeReportDto} instead.
 */
@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfficeReportDto {
    private Integer officeId;
    private String officeName;
    private Long totalOrders;
}
