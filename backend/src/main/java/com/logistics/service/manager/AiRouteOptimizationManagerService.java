package com.logistics.service.manager;

import com.logistics.config.AiServiceProperties;
import com.logistics.dto.ai.client.*;
import com.logistics.dto.manager.ai.*;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.repository.*;
import com.logistics.request.manager.ai.ManagerAiOptimizeRequest;
import com.logistics.response.ApiResponse;
import com.logistics.service.ai.AiServiceClient;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRouteOptimizationManagerService {

    private final EmployeeManagerService employeeManagerService;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;
    private final ShipperAssignmentRepository shipperAssignmentRepository;
    private final EmployeeLeaveRequestRepository leaveRequestRepository;
    private final AiRoutePlanRepository aiRoutePlanRepository;
    private final ShipperVehicleRepository shipperVehicleRepository;
    private final AiServiceClient aiServiceClient;
    private final AiServiceProperties aiServiceProperties;

    public ApiResponse<Map<String, Object>> previewDeliveryReadyOrders(Integer managerUserId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        List<Order> orders = findDeliveryReadyOrders(office.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("orderCount", orders.size());
        data.put("aiServiceHealthy", aiServiceClient.isHealthy());
        data.put("orders", orders.stream().map(this::toOrderPreview).limit(50).toList());
        return new ApiResponse<>(true, "Xem trước đơn sẵn sàng tối ưu", data);
    }

    @Transactional
    public ApiResponse<ManagerAiRoutePlanDetailDto> optimize(Integer managerUserId, ManagerAiOptimizeRequest request) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        Employee manager = resolveManagerEmployee(managerUserId, office);

        int capacity = aiServiceProperties.getDefaultCapacity();
        String startTime = request != null && request.getStartTime() != null
                ? request.getStartTime()
                : aiServiceProperties.getDefaultStartTime();

        List<Order> orders = findDeliveryReadyOrders(office.getId());
        if (orders.isEmpty()) {
            return new ApiResponse<>(false, "Không có đơn hàng sẵn sàng để tối ưu tuyến", null);
        }

        List<AiShipperInputDto> shippers = buildAvailableShippers(office, capacity, startTime);
        if (shippers.isEmpty()) {
            return new ApiResponse<>(false, "Không có shipper khả dụng (ACTIVE, có phân công khu vực, không nghỉ phép)", null);
        }

        AiRouteOptimizationRequestDto aiRequest = AiRouteOptimizationRequestDto.builder()
                .office(new AiOfficeLocationDto(
                        office.getId(),
                        office.getName(),
                        office.getLatitude().doubleValue(),
                        office.getLongitude().doubleValue()))
                .shippers(shippers)
                .orders(orders.stream().map(this::toAiOrder).toList())
                .options(Map.of("ortools_time_limit_seconds", 8))
                .build();

        AiRouteOptimizationResponseDto aiResponse = aiServiceClient.optimizeRoutes(aiRequest);
        AiRoutePlan plan = persistDraftPlan(office, manager, aiResponse, aiRequest);

        return new ApiResponse<>(true, "AI đã tạo đề xuất tuyến giao hàng", toDetailDto(plan, aiResponse.getUnassignedOrders()));
    }

    public ApiResponse<List<ManagerAiRoutePlanSummaryDto>> listPlans(Integer managerUserId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        List<ManagerAiRoutePlanSummaryDto> list = new ArrayList<>();
        aiRoutePlanRepository.findByOfficeIdAndStatusOrderByCreatedAtDesc(office.getId(), AiRoutePlanStatus.DRAFT)
                .stream()
                .map(this::toSummaryDto)
                .forEach(list::add);
        aiRoutePlanRepository.findByOfficeIdAndStatusOrderByCreatedAtDesc(office.getId(), AiRoutePlanStatus.CONFIRMED)
                .stream()
                .map(this::toSummaryDto)
                .forEach(list::add);
        return new ApiResponse<>(true, "Danh sách kế hoạch AI", list);
    }

    public ApiResponse<ManagerAiRoutePlanDetailDto> getPlan(Integer managerUserId, Long planId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        AiRoutePlan plan = aiRoutePlanRepository.findByIdAndOfficeIdWithDetails(planId, office.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kế hoạch AI"));
        plan.getRoutes().forEach(r -> {
            if (r.getStops() != null) {
                r.getStops().size();
            }
        });
        return new ApiResponse<>(true, "Chi tiết kế hoạch AI", toDetailDto(plan, List.of()));
    }

    @Transactional
    public ApiResponse<ManagerAiRoutePlanDetailDto> confirmPlan(Integer managerUserId, Long planId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        AiRoutePlan plan = aiRoutePlanRepository.findByIdAndOfficeIdWithDetails(planId, office.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kế hoạch AI"));

        if (plan.getStatus() != AiRoutePlanStatus.DRAFT) {
            return new ApiResponse<>(false, "Chỉ có thể xác nhận kế hoạch ở trạng thái DRAFT", null);
        }

        for (AiRoutePlanRoute route : plan.getRoutes()) {
            Employee shipperEmployee = employeeRepository.findById(route.getShipperEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Shipper không tồn tại: " + route.getShipperEmployeeId()));

            for (AiRoutePlanStop stop : route.getStops()) {
                Order order = stop.getOrder();
                if (order.getEmployee() != null && !Objects.equals(order.getEmployee().getId(), shipperEmployee.getId())) {
                    return new ApiResponse<>(false,
                            "Đơn " + order.getTrackingNumber() + " đã được gán cho shipper khác", null);
                }
                order.setEmployee(shipperEmployee);
                if (order.getStatus() == OrderStatus.AT_DEST_OFFICE) {
                    order.setStatus(OrderStatus.READY_FOR_PICKUP);
                }
                orderRepository.save(order);
            }
        }

        plan.setStatus(AiRoutePlanStatus.CONFIRMED);
        plan.setConfirmedAt(LocalDateTime.now());
        aiRoutePlanRepository.save(plan);

        return new ApiResponse<>(true, "Đã xác nhận và gán đơn theo tuyến AI", toDetailDto(plan, List.of()));
    }

    @Transactional
    public ApiResponse<Boolean> cancelPlan(Integer managerUserId, Long planId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        AiRoutePlan plan = aiRoutePlanRepository.findByIdAndOfficeIdWithDetails(planId, office.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kế hoạch AI"));
        if (plan.getStatus() != AiRoutePlanStatus.DRAFT) {
            return new ApiResponse<>(false, "Chỉ hủy được kế hoạch DRAFT", false);
        }
        plan.setStatus(AiRoutePlanStatus.CANCELLED);
        aiRoutePlanRepository.save(plan);
        return new ApiResponse<>(true, "Đã hủy kế hoạch AI", true);
    }

    private Employee resolveManagerEmployee(Integer managerUserId, Office office) {
        List<Employee> employees = employeeRepository.findByUserId(managerUserId);
        return employees.stream()
                .filter(e -> e.getOffice() != null && Objects.equals(e.getOffice().getId(), office.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên quản lý"));
    }

    private List<Order> findDeliveryReadyOrders(Integer officeId) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("toOffice").get("id"), officeId));
            predicates.add(cb.equal(root.get("status"), OrderStatus.AT_DEST_OFFICE));
            predicates.add(cb.isNull(root.get("employee")));
            predicates.add(cb.isNotNull(root.get("recipientLatitude")));
            predicates.add(cb.isNotNull(root.get("recipientLongitude")));
            predicates.add(cb.notEqual(root.get("recipientLatitude"), 0));
            predicates.add(cb.notEqual(root.get("recipientLongitude"), 0));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return orderRepository.findAll(spec);
    }

    private List<AiShipperInputDto> buildAvailableShippers(Office office, int capacity, String startTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        List<User> shipperUsers = employeeRepository.findAll().stream()
                .filter(e -> e.getOffice() != null && Objects.equals(e.getOffice().getId(), office.getId()))
                .filter(e -> e.getStatus() == EmployeeStatus.ACTIVE)
                .filter(e -> e.getUser() != null)
                .filter(e -> e.getAccountRole() != null
                        && e.getAccountRole().getRole() != null
                        && "Shipper".equalsIgnoreCase(e.getAccountRole().getRole().getName()))
                .map(Employee::getUser)
                .distinct()
                .toList();

        List<AiShipperInputDto> result = new ArrayList<>();
        for (User user : shipperUsers) {
            List<Employee> emps = employeeRepository.findByUserId(user.getId());
            Employee employee = emps.stream()
                    .filter(e -> e.getOffice() != null && Objects.equals(e.getOffice().getId(), office.getId()))
                    .findFirst()
                    .orElse(null);
            if (employee == null || employee.getStatus() != EmployeeStatus.ACTIVE) {
                continue;
            }
            if (leaveRequestRepository.existsApprovedLeaveOnDate(
                    employee.getId(), today, LeaveRequestStatus.APPROVED)) {
                continue;
            }

            ShipperVehicle vehicle = shipperVehicleRepository.findByShipperId(employee.getId())
                    .orElseGet(() -> createDefaultVehicle(employee));
            if (vehicle.getStatus() != ShipperVehicleStatus.ACTIVE) {
                continue;
            }

            int maxOrders = vehicle.getMaxOrders() != null && vehicle.getMaxOrders() > 0
                    ? vehicle.getMaxOrders()
                    : capacity;
            int currentOrders = vehicle.getCurrentOrders() != null ? Math.max(0, vehicle.getCurrentOrders()) : 0;
            int remainingOrders = Math.max(0, maxOrders - currentOrders);
            if (remainingOrders <= 0) {
                continue;
            }

            double maxWeightKg = vehicle.getMaxWeightKg() != null && vehicle.getMaxWeightKg() > 0
                    ? vehicle.getMaxWeightKg()
                    : 35.0;
            double currentWeightKg = vehicle.getCurrentWeightKg() != null
                    ? Math.max(0.0, vehicle.getCurrentWeightKg().doubleValue())
                    : 0.0;
            double remainingWeightKg = Math.max(0.0, maxWeightKg - currentWeightKg);
            if (remainingWeightKg <= 0) {
                continue;
            }

            List<ShipperAssignment> assignments = shipperAssignmentRepository.findActiveByShipperId(user.getId(), now);
            if (assignments.isEmpty()) {
                continue;
            }

            String name = user.getFirstName() + " " + user.getLastName();
            List<AiShipperAssignmentAreaDto> areas = assignments.stream()
                    .map(a -> new AiShipperAssignmentAreaDto(a.getWardCode(), a.getCityCode()))
                    .toList();

            result.add(AiShipperInputDto.builder()
                    .id(user.getId())
                    .employeeId(employee.getId())
                    .name(name.trim())
                    .capacity(remainingOrders)
                    .speedKmh(aiServiceProperties.getDefaultSpeedKmh())
                    .fuelCostPerKm(aiServiceProperties.getDefaultFuelCostPerKm())
                    .startTime(startTime)
                    .vehicleType(vehicle.getVehicleType() != null ? vehicle.getVehicleType().name() : null)
                    .maxWeightKg((int) Math.round(maxWeightKg))
                    .remainingWeightKg(remainingWeightKg)
                    .batteryLevel(vehicle.getBatteryLevel())
                    .assignments(areas)
                    .build());
        }
        return result;
    }

    private AiOrderInputDto toAiOrder(Order order) {
        Double weightKg = normalizeOrderWeight(order);
        return AiOrderInputDto.builder()
                .id(order.getId())
                .trackingNumber(order.getTrackingNumber())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .recipientAddress(order.getRecipientFullAddress())
                .recipientWardCode(order.getRecipientWardCode())
                .recipientCityCode(order.getRecipientCityCode())
                .latitude(order.getRecipientLatitude())
                .longitude(order.getRecipientLongitude())
                .codAmount(order.getCod() != null ? order.getCod() : 0)
                .priority("NORMAL")
                .weightKg(weightKg)
                .build();
    }

    private ShipperVehicle createDefaultVehicle(Employee employee) {
        ShipperVehicle vehicle = new ShipperVehicle();
        vehicle.setShipper(employee);
        vehicle.setVehicleType(ShipperVehicleType.MOTORBIKE);
        vehicle.setMaxOrders(20);
        vehicle.setMaxWeightKg(35);
        vehicle.setCurrentOrders(0);
        vehicle.setCurrentWeightKg(BigDecimal.ZERO);
        vehicle.setBatteryLevel(null);
        vehicle.setStatus(ShipperVehicleStatus.ACTIVE);
        vehicle.setNotes("Auto-created default vehicle");
        return shipperVehicleRepository.save(vehicle);
    }

    private Double normalizeOrderWeight(Order order) {
        if (order.getWeight() == null || order.getWeight().doubleValue() <= 0) {
            log.warn("Order {} has invalid weight {}, fallback to 1.0kg", order.getId(), order.getWeight());
            return 1.0;
        }
        return order.getWeight().doubleValue();
    }

    private AiRoutePlan persistDraftPlan(
            Office office,
            Employee manager,
            AiRouteOptimizationResponseDto aiResponse,
            AiRouteOptimizationRequestDto aiRequest) {

        AiRoutePlan plan = new AiRoutePlan();
        plan.setOffice(office);
        plan.setManagerEmployee(manager);
        plan.setStatus(AiRoutePlanStatus.DRAFT);
        plan.setPlanCode("AI-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        plan.setOptimizationNote(aiResponse.getMessage());

        if (aiResponse.getSummary() != null) {
            AiOptimizationSummaryDto s = aiResponse.getSummary();
            plan.setTotalDistanceKm(toBd(s.getTotalDistanceKm()));
            plan.setTotalDurationMinutes(toBd(s.getTotalDurationMinutes()));
            plan.setTotalFuelCost(toBd(s.getTotalFuelCost()));
            plan.setTotalCod(s.getTotalCod());
            plan.setUnassignedCount(s.getUnassignedOrderCount() != null ? s.getUnassignedOrderCount() : 0);
        }

        Set<Integer> orderIds = new HashSet<>();
        for (AiShipperRouteOutputDto routeDto : aiResponse.getRoutes()) {
            if (routeDto.getStops() == null) {
                continue;
            }
            for (AiRouteStopOutputDto stopDto : routeDto.getStops()) {
                if (stopDto.getOrderId() != null) {
                    orderIds.add(stopDto.getOrderId());
                }
            }
        }
        Map<Integer, Order> orderMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        for (AiShipperRouteOutputDto routeDto : aiResponse.getRoutes()) {
            AiRoutePlanRoute route = new AiRoutePlanRoute();
            route.setPlan(plan);
            route.setShipperUserId(routeDto.getShipperId());
            route.setShipperEmployeeId(routeDto.getEmployeeId());
            route.setShipperName(routeDto.getShipperName());
            route.setRouteSequence(routeDto.getRouteSequence() != null ? routeDto.getRouteSequence() : 0);
            route.setEstimatedDistanceKm(toBd(routeDto.getEstimatedDistanceKm()));
            route.setEstimatedDurationMinutes(toBd(routeDto.getEstimatedDurationMinutes()));
            route.setFuelCost(toBd(routeDto.getFuelCost()));
            route.setTotalCod(routeDto.getTotalCod());
            route.setEncodedPolyline(routeDto.getEncodedPolyline());
            route.setStartTime(routeDto.getStartTime());

            List<AiRoutePlanStop> stops = new ArrayList<>();
            if (routeDto.getStops() != null) {
                for (AiRouteStopOutputDto stopDto : routeDto.getStops()) {
                    Order order = orderMap.get(stopDto.getOrderId());
                    if (order == null) {
                        continue;
                    }
                    AiRoutePlanStop stop = new AiRoutePlanStop();
                    stop.setRoute(route);
                    stop.setOrder(order);
                    stop.setStopSequence(stopDto.getStopSequence());
                    stop.setTrackingNumber(stopDto.getTrackingNumber());
                    stop.setRecipientName(stopDto.getRecipientName());
                    stop.setRecipientPhone(stopDto.getRecipientPhone());
                    stop.setRecipientAddress(stopDto.getRecipientAddress());
                    stop.setRecipientLatitude(stopDto.getLatitude());
                    stop.setRecipientLongitude(stopDto.getLongitude());
                    stop.setCodAmount(stopDto.getCodAmount());
                    stop.setPriority(stopDto.getPriority());
                    stop.setEtaTime(stopDto.getEtaTime());
                    stop.setEtaMinutesFromStart(stopDto.getEtaMinutesFromStart());
                    stop.setLegDistanceKm(toBd(stopDto.getLegDistanceKm()));
                    stops.add(stop);
                }
            }
            route.setStops(stops);
            route.setStopCount(stops.size());
            plan.getRoutes().add(route);
        }

        return aiRoutePlanRepository.save(plan);
    }

    private ManagerAiRoutePlanDetailDto toDetailDto(AiRoutePlan plan, List<AiUnassignedOrderOutputDto> unassignedFromAi) {
        List<ManagerAiShipperRouteDto> routes = plan.getRoutes().stream()
                .sorted(Comparator.comparing(AiRoutePlanRoute::getRouteSequence))
                .map(r -> ManagerAiShipperRouteDto.builder()
                        .routeId(r.getId())
                        .shipperUserId(r.getShipperUserId())
                        .shipperEmployeeId(r.getShipperEmployeeId())
                        .shipperName(r.getShipperName())
                        .routeSequence(r.getRouteSequence())
                        .estimatedDistanceKm(r.getEstimatedDistanceKm())
                        .estimatedDurationMinutes(r.getEstimatedDurationMinutes())
                        .fuelCost(r.getFuelCost())
                        .totalCod(r.getTotalCod())
                        .encodedPolyline(r.getEncodedPolyline())
                        .startTime(r.getStartTime())
                        .stopCount(r.getStopCount())
                        .stops(r.getStops().stream()
                                .sorted(Comparator.comparing(AiRoutePlanStop::getStopSequence))
                                .map(s -> ManagerAiRouteStopDto.builder()
                                        .stopId(s.getId())
                                        .orderId(s.getOrder().getId())
                                        .stopSequence(s.getStopSequence())
                                        .trackingNumber(s.getTrackingNumber())
                                        .recipientName(s.getRecipientName())
                                        .recipientPhone(s.getRecipientPhone())
                                        .recipientAddress(s.getRecipientAddress())
                                        .latitude(s.getRecipientLatitude())
                                        .longitude(s.getRecipientLongitude())
                                        .codAmount(s.getCodAmount())
                                        .priority(s.getPriority())
                                        .etaTime(s.getEtaTime())
                                        .etaMinutesFromStart(s.getEtaMinutesFromStart())
                                        .build())
                                .toList())
                        .build())
                .toList();

        List<ManagerAiUnassignedOrderDto> unassigned = unassignedFromAi.stream()
                .map(u -> ManagerAiUnassignedOrderDto.builder()
                        .orderId(u.getOrderId())
                        .trackingNumber(u.getTrackingNumber())
                        .reason(u.getReason())
                        .build())
                .toList();

        return ManagerAiRoutePlanDetailDto.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .status(plan.getStatus())
                .officeId(plan.getOffice().getId())
                .officeName(plan.getOffice().getName())
                .totalDistanceKm(plan.getTotalDistanceKm())
                .totalDurationMinutes(plan.getTotalDurationMinutes())
                .totalFuelCost(plan.getTotalFuelCost())
                .totalCod(plan.getTotalCod())
                .unassignedCount(plan.getUnassignedCount())
                .optimizationNote(plan.getOptimizationNote())
                .createdAt(plan.getCreatedAt())
                .confirmedAt(plan.getConfirmedAt())
                .routes(routes)
                .unassignedOrders(unassigned)
                .build();
    }

    private ManagerAiRoutePlanSummaryDto toSummaryDto(AiRoutePlan plan) {
        return ManagerAiRoutePlanSummaryDto.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .status(plan.getStatus())
                .totalDistanceKm(plan.getTotalDistanceKm())
                .totalDurationMinutes(plan.getTotalDurationMinutes())
                .totalFuelCost(plan.getTotalFuelCost())
                .totalCod(plan.getTotalCod())
                .unassignedCount(plan.getUnassignedCount())
                .routeCount(plan.getRoutes() != null ? plan.getRoutes().size() : 0)
                .createdAt(plan.getCreatedAt())
                .confirmedAt(plan.getConfirmedAt())
                .build();
    }

    private Map<String, Object> toOrderPreview(Order order) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", order.getId());
        m.put("trackingNumber", order.getTrackingNumber());
        m.put("recipientName", order.getRecipientName());
        m.put("recipientWardCode", order.getRecipientWardCode());
        m.put("recipientCityCode", order.getRecipientCityCode());
        m.put("cod", order.getCod());
        return m;
    }

    private BigDecimal toBd(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
