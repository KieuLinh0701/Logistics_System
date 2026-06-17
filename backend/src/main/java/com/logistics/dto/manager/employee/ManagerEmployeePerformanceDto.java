package com.logistics.dto.manager.employee;

import com.logistics.enums.EmployeeShift;
import com.logistics.enums.EmployeeStatus;

public interface ManagerEmployeePerformanceDto {
  Integer getId();
  String getEmployeeName();
  String getEmployeeCode();
  String getEmployeeRole();
  String getEmployeePhone();
  EmployeeStatus getEmployeeStatus();
  EmployeeShift getEmployeeShift();
  Long getTotalShipments();
  Long getTotalOrders();
  Long getCompletedOrders();
  Double getAvgTimePerOrder();

  default Double getCompletionRate() {
    if (getTotalOrders() == null || getTotalOrders() == 0) return 0.0;
    return getCompletedOrders() * 100.0 / getTotalOrders();
  }
}