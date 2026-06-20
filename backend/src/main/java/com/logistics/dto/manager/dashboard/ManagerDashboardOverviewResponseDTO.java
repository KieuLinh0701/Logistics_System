package com.logistics.dto.manager.dashboard;

import com.logistics.enums.EmployeeShift;
import com.logistics.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagerDashboardOverviewResponseDTO {
    private ManagerEmployeeStatsDTO employees;//
    private ManagerIncidentStatsDTO incidents;// 
    private ManagerOrderStatsDTO orders;
    private ManagerPaymentSubmissionBatchStatsDto payments;//
    private ManagerShipmentStatsDTO shipments;//
    private ManagerShippingRequestStatsDTO shippingRequests;//
    private ManagerVehicleStatsDto vehicles;//
    private Map<VehicleType, Long> vehicleCounts;//
    private Map<EmployeeShift, Long> employeeShiftCounts;//
}