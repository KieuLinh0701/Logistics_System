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
import com.logistics.repository.OrderProductRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.PaymentSubmissionBatchRepository;
import com.logistics.repository.ProductRepository;
import com.logistics.repository.ShipmentRepository;
import com.logistics.repository.ShippingRequestRepository;
import com.logistics.repository.VehicleRepository;
import com.logistics.response.ApiResponse;

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

    public ApiResponse<ManagerDashboardOverviewResponseDTO> getOverview(Integer userId) {
        try {
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

            return new ApiResponse<>(true, "Lấy thông tin tổng quan thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy tổng quan: " + e.getMessage(), null);
        }
    }

    // public ApiResponse<UserDashboardChartResponseDTO> getChart(Integer userId,
    // SearchRequest request) {
    // try {
    // LocalDateTime startDate = request.getStartDate() != null &&
    // !request.getStartDate().isBlank()
    // ? LocalDateTime.parse(request.getStartDate())
    // : null;

    // LocalDateTime endDate = request.getEndDate() != null &&
    // !request.getEndDate().isBlank()
    // ? LocalDateTime.parse(request.getEndDate())
    // : null;

    // List<UserTopProductItemDto> topSelling =
    // orderProductRepository.findTopSellingProducts(
    // userId,
    // OrderStatus.DELIVERED,
    // startDate,
    // endDate,
    // PageRequest.of(0, 5));

    // List<UserTopProductItemDto> topReturned =
    // orderProductRepository.findTopReturnedProducts(
    // userId,
    // List.of(OrderStatus.RETURNING, OrderStatus.RETURNED),
    // startDate,
    // endDate,
    // PageRequest.of(0, 5));

    // List<UserOrderTimelineDTO> orderTimelineDTOs = getOrderTimeline(userId,
    // startDate,
    // endDate);

    // UserDashboardChartResponseDTO data = new UserDashboardChartResponseDTO();
    // data.setTopSelling(topSelling);
    // data.setTopReturned(topReturned);
    // data.setOrderTimelines(orderTimelineDTOs);

    // return new ApiResponse<>(true, "Lấy thông tin biểu đồ thành công", data);
    // } catch (Exception e) {
    // return new ApiResponse<>(false, "Lỗi khi lấy biểu đồ: " + e.getMessage(),
    // null);
    // }
    // }

    // public List<UserOrderTimelineDTO> getOrderTimeline(
    // Integer userId,
    // LocalDateTime startDate,
    // LocalDateTime endDate) {
    // List<UserCreatedOrderCountDTO> createdList =
    // orderRepository.countCreatedOrdersByDate(userId, startDate,
    // endDate);

    // List<UserDeliveredOrderCountDTO> deliveredList =
    // orderRepository.countDeliveredOrdersByDate(userId, startDate,
    // endDate);

    // Map<LocalDate, UserOrderTimelineDTO> map = new TreeMap<>();

    // for (UserCreatedOrderCountDTO c : createdList) {
    // LocalDate date = c.getDate().toLocalDate();
    // map.put(
    // date,
    // new UserOrderTimelineDTO(date, c.getCreatedCount(), 0L));
    // }

    // for (UserDeliveredOrderCountDTO d : deliveredList) {
    // LocalDate date = d.getDate().toLocalDate();
    // map.compute(
    // date,
    // (k, v) -> {
    // if (v == null) {
    // return new UserOrderTimelineDTO(date, 0L, d.getDeliveredCount());
    // }
    // v.setDeliveredCount(d.getDeliveredCount());
    // return v;
    // });
    // }

    // return new ArrayList<>(map.values());
    // }
}