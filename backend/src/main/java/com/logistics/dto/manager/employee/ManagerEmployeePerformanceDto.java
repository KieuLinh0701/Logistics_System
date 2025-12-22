package com.logistics.dto.manager.employee;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerEmployeePerformanceDto {

  private Integer id;

  private String employeeName;
  private String employeeCode;
  private String employeeRole;
  private String employeePhone;
  private String employeeStatus; 
  private String employeeShift;

  private Long totalShipments;
  private Long totalOrders;
  private Long completedOrders;

  private Double avgTimePerOrder;

  public Double getCompletionRate() {
    if (totalOrders == 0) return 0.0;
    return completedOrders * 100.0 / totalOrders;
  }
}