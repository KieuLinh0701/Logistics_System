package com.logistics.dto.manager.dashboard;

import java.util.Map;

import com.logistics.enums.EmployeeShift;
import com.logistics.enums.VehicleType;

import lombok.*;

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