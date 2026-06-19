package com.logistics.service.manager;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.dto.manager.dashboard.ManagerDashboardOverviewResponseDTO;
import com.logistics.dto.manager.dashboard.ManagerEmployeeStatsDTO;
import com.logistics.dto.manager.dashboard.ManagerIncidentStatsDTO;
import com.logistics.dto.manager.dashboard.ManagerOrderStatsDTO;
import com.logistics.dto.manager.dashboard.ManagerPaymentSubmissionBatchStatsDto;
import com.logistics.dto.manager.dashboard.ManagerShipmentStatsDTO;
import com.logistics.dto.manager.dashboard.ManagerShippingRequestStatsDTO;
import com.logistics.dto.manager.dashboard.ManagerVehicleStatsDto;
import com.logistics.entity.Office;
import com.logistics.enums.EmployeeShift;
import com.logistics.enums.VehicleType;
import com.logistics.repository.EmployeeRepository;
import com.logistics.repository.IncidentReportRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionBatchRepository;
import com.logistics.repository.ShipmentRepository;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardManagerService {

    private final VehicleRepository vehicleRepository;
    private final ShippingRequestRepository shippingRequestRepository;
    private final ShipmentRepository shipmentRepository;
    private final PaymentSubmissionBatchRepository paymentSubmissionBatchRepository;
    private final IncidentReportRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;

    private final EmployeeManagerService employeeManagerService;

    public ManagerDashboardOverviewResponseDTO getOverview(Integer userId) {
            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);
            Integer officeId = userOffice.getId();

            List<Object[]> rows = vehicleRepository.countVehiclesByTypeForOffice(officeId);
            Map<VehicleType, Long> vehicleCounts = new EnumMap<>(VehicleType.class);

            for (VehicleType type : VehicleType.values()) {
                vehicleCounts.put(type, 0L);
            }

            if (rows != null) {
                for (Object[] row : rows) {
                    if (row != null && row[0] instanceof VehicleType && row[1] instanceof Long) {
                        VehicleType type = (VehicleType) row[0];
                        Long count = (Long) row[1];
                        vehicleCounts.put(type, count);
                    }
                }
            }

            List<Object[]> rowEmployees = employeeRepository.countActiveEmployeesByShiftForOffice(officeId);
            Map<EmployeeShift, Long> employeeShiftCounts = new EnumMap<>(EmployeeShift.class);

            for (EmployeeShift type : EmployeeShift.values()) {
                employeeShiftCounts.put(type, 0L);
            }

            if (rows != null) {
                for (Object[] rowEmployee : rowEmployees) {
                    if (rowEmployee != null && rowEmployee[0] instanceof EmployeeShift
                            && rowEmployee[1] instanceof Long) {
                        EmployeeShift shift = (EmployeeShift) rowEmployee[0];
                        Long count = (Long) rowEmployee[1];
                        employeeShiftCounts.put(shift, count);
                    }
                }
            }

            ManagerVehicleStatsDto vehicles = vehicleRepository.getVehicleStatsByOffice(officeId);
            if (vehicles == null)
                vehicles = new ManagerVehicleStatsDto();

            ManagerShippingRequestStatsDTO shippingRequests = shippingRequestRepository
                    .getShippingRequestStatsByOffice(officeId);
            if (shippingRequests == null)
                shippingRequests = new ManagerShippingRequestStatsDTO();

            ManagerShipmentStatsDTO shipments = shipmentRepository.getShipmentStatsByOffice(officeId);
            if (shipments == null)
                shipments = new ManagerShipmentStatsDTO();

            ManagerPaymentSubmissionBatchStatsDto payments = paymentSubmissionBatchRepository
                    .getPaymentSubmissionBatchStatsByOffice(officeId);
            if (payments == null)
                payments = new ManagerPaymentSubmissionBatchStatsDto();

            ManagerIncidentStatsDTO incidents = incidentRepository.getIncidentStatsByOffice(officeId);
            if (incidents == null)
                incidents = new ManagerIncidentStatsDTO();

            ManagerEmployeeStatsDTO employees = employeeRepository.getEmployeeStatsByOffice(officeId);
            if (employees == null)
                employees = new ManagerEmployeeStatsDTO();

            ManagerOrderStatsDTO orders = orderRepository.getOrderStatsByOfficeId(officeId);
            if (orders == null)
                orders = new ManagerOrderStatsDTO();

            ManagerDashboardOverviewResponseDTO data = new ManagerDashboardOverviewResponseDTO();
            data.setVehicleCounts(vehicleCounts);
            data.setVehicles(vehicles);
            data.setShippingRequests(shippingRequests); 
            data.setShipments(shipments);
            data.setPayments(payments);
            data.setIncidents(incidents);
            data.setEmployees(employees);
            data.setOrders(orders);
            data.setEmployeeShiftCounts(employeeShiftCounts);

            return data;
    }
}