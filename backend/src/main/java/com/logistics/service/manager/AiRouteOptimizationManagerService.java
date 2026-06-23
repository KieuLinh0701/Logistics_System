package com.logistics.service.manager;

import com.logistics.config.properties.AiServiceProperties;
import com.logistics.dto.ai.*;
import com.logistics.dto.manager.ai.*;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.AiRouteErrorCode;
import com.logistics.repository.*;
import com.logistics.request.manager.ai.ManagerAiOptimizeRequest;
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
import java.util.function.Function;
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

    public Map<String, Object> previewDeliveryReadyOrders(Integer managerUserId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        List<Order> orders = findDeliveryReadyOrders(office.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("orderCount", orders.size());
        data.put("aiServiceHealthy", aiServiceClient.isHealthy());
        data.put("orders", orders.stream().map(this::toOrderPreview).limit(50).toList());
        return data;
    }

    @Transactional
    public ManagerAiRoutePlanDetailDto optimize(Integer managerUserId, ManagerAiOptimizeRequest request) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        Employee manager = resolveManagerEmployee(managerUserId, office);

        int capacity = aiServiceProperties.getDefaultCapacity();
        String startTime = request != null && request.getStartTime() != null
                ? request.getStartTime()
                : aiServiceProperties.getDefaultStartTime();

        boolean returnToOffice = request != null && request.getReturnToOffice() != null
                ? request.getReturnToOffice()
                : true;
        RouteMode routeMode = request != null && request.getRouteMode() != null
                ? request.getRouteMode()
                : RouteMode.CLOSED_LOOP;

        // Lấy delivery orders
        List<Order> deliveryOrders = findDeliveryReadyOrders(office.getId());
        List<AiRouteStopInputDto> deliveryStops = deliveryOrders.stream()
                .map(this::toAiRouteStop).toList();

        if (deliveryStops.isEmpty()) {
            throw new AppException(AiRouteErrorCode.AI_NO_ORDERS_READY);
        }

        List<AiShipperInputDto> shippers = buildAvailableShippers(office, capacity, startTime);
        if (shippers.isEmpty()) {
            throw new AppException(AiRouteErrorCode.AI_NO_AVAILABLE_SHIPPERS);
        }

        // Build AI request với closed loop
        AiLocationDto startLocation = new AiLocationDto();
        startLocation.setType("OFFICE");
        startLocation.setId(office.getId());
        startLocation.setName(office.getName());
        startLocation.setLatitude(office.getLatitude().doubleValue());
        startLocation.setLongitude(office.getLongitude().doubleValue());

        AiLocationDto endLocation = returnToOffice ? startLocation : null;

        AiRouteOptimizationRequestDto aiRequest = AiRouteOptimizationRequestDto.builder()
                .office(new AiOfficeLocationDto(
                        office.getId(),
                        office.getName(),
                        office.getDetail(),
                        office.getLatitude().doubleValue(),
                        office.getLongitude().doubleValue()
                ))
                .startLocation(startLocation)
                .endLocation(endLocation)
                .returnToOffice(returnToOffice)
                .routeMode(routeMode.name())
                .optimizationScope(RouteOptimizationScope.MANAGER_GLOBAL.name())
                .shippers(shippers)
                .stops(deliveryStops)
                .options(Map.of("ortools_time_limit_seconds", 8))
                .build();

        AiRouteOptimizationResponseDto aiResponse = aiServiceClient.optimizeRoutes(aiRequest);

        AiRoutePlan plan = persistDraftPlan(office, manager, deliveryOrders, aiResponse, aiRequest, routeMode, returnToOffice);

        return toDetailDto(plan, aiResponse.getUnassignedOrders());
    }

    public List<ManagerAiRoutePlanSummaryDto> listPlans(Integer managerUserId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        List<ManagerAiRoutePlanSummaryDto> list = new ArrayList<>();
        aiRoutePlanRepository.findByOfficeIdAndStatusOrderByCreatedAtDesc(office.getId(), AiRoutePlanStatus.DRAFT)
                .stream().map(this::toSummaryDto).forEach(list::add);
        aiRoutePlanRepository.findByOfficeIdAndStatusOrderByCreatedAtDesc(office.getId(), AiRoutePlanStatus.CONFIRMED)
                .stream().map(this::toSummaryDto).forEach(list::add);
        return list;
    }

    @Transactional(readOnly = true)
    public ManagerAiRoutePlanDetailDto getPlan(Integer managerUserId, Long planId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);

        // Fetch plan + office/manager nhẹ nhàng, không join routes/stops
        AiRoutePlan plan = aiRoutePlanRepository.findByIdAndOfficeIdWithDetails(planId, office.getId())
                .orElseThrow(() -> new AppException(AiRouteErrorCode.AI_PLAN_NOT_FOUND));

        // Fetch routes riêng (tránh MultipleBagFetchException)
        List<AiRoutePlanRoute> routes = aiRoutePlanRepository.findRoutesByPlanId(planId);

        // Fetch stops riêng cho tất cả routes (tránh N+1)
        List<Long> routeIds = routes.stream().map(AiRoutePlanRoute::getId).toList();
        List<AiRoutePlanStop> allStops = routeIds.isEmpty()
                ? List.of()
                : aiRoutePlanRepository.findStopsByRouteIds(routeIds);

        // Map stops theo routeId
        Map<Long, List<AiRoutePlanStop>> stopsByRouteId = allStops.stream()
                .collect(Collectors.groupingBy(s -> s.getRoute().getId()));

        // Gán stops vào routes (trong memory, không trigger Hibernate)
        for (AiRoutePlanRoute route : routes) {
            route.setStops(new ArrayList<>(stopsByRouteId.getOrDefault(route.getId(), List.of())));
        }
        plan.setRoutes(routes);

        log.debug("getPlan: planId={} officeId={} routes={} stopsTotal={}",
                planId, office.getId(), routes.size(), allStops.size());

        return toDetailDto(plan, List.of());
    }

    @Transactional
    public ManagerAiRoutePlanDetailDto confirmPlan(Integer managerUserId, Long planId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        AiRoutePlan plan = aiRoutePlanRepository.findByIdAndOfficeIdWithDetails(planId, office.getId())
                .orElseThrow(() -> new AppException(AiRouteErrorCode.AI_PLAN_NOT_FOUND));

        if (plan.getStatus() != AiRoutePlanStatus.DRAFT) {
            throw new AppException(AiRouteErrorCode.AI_INVALID_PLAN_STATUS);
        }

        for (AiRoutePlanRoute route : plan.getRoutes()) {
            Employee shipperEmployee = employeeRepository.findById(route.getShipperEmployeeId())
                    .orElseThrow(() -> new AppException(AiRouteErrorCode.AI_SHIPPER_NOT_FOUND));

            for (AiRoutePlanStop stop : route.getStops()) {
                RouteStopType stopType = stop.getStopType();
                Order order = stop.getOrder();

                // RETURN_TO_OFFICE stop: không có order, bỏ qua
                if (stopType == RouteStopType.RETURN_TO_OFFICE) {
                    continue;
                }

                if (order != null) {
                    if (order.getEmployee() != null && !Objects.equals(order.getEmployee().getId(), shipperEmployee.getId())) {
                        throw new AppException(AiRouteErrorCode.AI_ORDER_ASSIGNED_TO_OTHER);
                    }
                    order.setEmployee(shipperEmployee);

                    // DELIVERY: chuyển AT_DEST_OFFICE -> READY_FOR_PICKUP
                    if (stopType == RouteStopType.DELIVERY) {
                        if (order.getStatus() == OrderStatus.AT_DEST_OFFICE) {
                            order.setStatus(OrderStatus.READY_FOR_PICKUP);
                        }
                    }
                    // PICKUP: giữ nguyên status hoặc chuyển CONFIRMED -> READY_FOR_PICKUP nếu cần
                    else if (stopType == RouteStopType.PICKUP) {
                        if (order.getStatus() == OrderStatus.CONFIRMED) {
                            order.setStatus(OrderStatus.READY_FOR_PICKUP);
                        }
                    }
                    orderRepository.save(order);
                }
            }
        }

        plan.setStatus(AiRoutePlanStatus.CONFIRMED);
        plan.setConfirmedAt(LocalDateTime.now());
        aiRoutePlanRepository.save(plan);

        log.info("confirmPlan: planId={} routeCount={} stopCount={}",
                planId,
                plan.getRoutes().size(),
                plan.getRoutes().stream().mapToInt(r -> r.getStops().size()).sum());

        return toDetailDto(plan, List.of());
    }

    @Transactional
    public void cancelPlan(Integer managerUserId, Long planId) {
        Office office = employeeManagerService.getManagedOfficeByUserId(managerUserId);
        AiRoutePlan plan = aiRoutePlanRepository.findByIdAndOfficeIdWithDetails(planId, office.getId())
                .orElseThrow(() -> new AppException(AiRouteErrorCode.AI_PLAN_NOT_FOUND));
        if (plan.getStatus() != AiRoutePlanStatus.DRAFT) {
            throw new AppException(AiRouteErrorCode.AI_INVALID_PLAN_STATUS);
        }
        plan.setStatus(AiRoutePlanStatus.CANCELLED);
        aiRoutePlanRepository.save(plan);
    }

    private Employee resolveManagerEmployee(Integer managerUserId, Office office) {
        List<Employee> employees = employeeRepository.findByUserId(managerUserId);
        return employees.stream()
                .filter(e -> e.getOffice() != null && Objects.equals(e.getOffice().getId(), office.getId()))
                .findFirst()
                .orElseThrow(() -> new AppException(AiRouteErrorCode.AI_MANAGER_RESOLVE_FAILED));
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
                    .findFirst().orElse(null);
            if (employee == null || employee.getStatus() != EmployeeStatus.ACTIVE) continue;
            if (leaveRequestRepository.existsApprovedLeaveOnDate(employee.getId(), today, LeaveRequestStatus.APPROVED)) continue;

            ShipperVehicle vehicle = shipperVehicleRepository.findByShipperId(employee.getId())
                    .orElseGet(() -> createDefaultVehicle(employee));
            if (vehicle.getStatus() != ShipperVehicleStatus.ACTIVE) continue;

            int maxOrders = vehicle.getMaxOrders() != null && vehicle.getMaxOrders() > 0
                    ? vehicle.getMaxOrders() : capacity;
            int currentOrders = vehicle.getCurrentOrders() != null ? Math.max(0, vehicle.getCurrentOrders()) : 0;
            int remainingOrders = Math.max(0, maxOrders - currentOrders);
            if (remainingOrders <= 0) continue;

            double maxWeightKg = vehicle.getMaxWeightKg() != null && vehicle.getMaxWeightKg() > 0
                    ? vehicle.getMaxWeightKg() : 35.0;
            double currentWeightKg = vehicle.getCurrentWeightKg() != null
                    ? Math.max(0.0, vehicle.getCurrentWeightKg().doubleValue()) : 0.0;
            double remainingWeightKg = Math.max(0.0, maxWeightKg - currentWeightKg);
            if (remainingWeightKg <= 0) continue;

            List<ShipperAssignment> assignments = shipperAssignmentRepository.findActiveByShipperId(user.getId(), now);
            if (assignments.isEmpty()) continue;

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

    private AiRouteStopInputDto toAiRouteStop(Order order) {
        Double weightKg = normalizeOrderWeight(order);
        return AiRouteStopInputDto.builder()
                .stopId(order.getId().longValue())
                .orderId(order.getId())
                .trackingNumber(order.getTrackingNumber())
                .stopType("DELIVERY")
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .address(order.getRecipientFullAddress())
                .wardCode(order.getRecipientWardCode())
                .cityCode(order.getRecipientCityCode())
                .latitude(order.getRecipientLatitude())
                .longitude(order.getRecipientLongitude())
                .codAmount(order.getCod() != null ? order.getCod() : 0)
                .priority("NORMAL")
                .serviceTimeMinutes(5)
                .weightKg(weightKg)
                .build();
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
            List<Order> deliveryOrders,
            AiRouteOptimizationResponseDto aiResponse,
            AiRouteOptimizationRequestDto aiRequest,
            RouteMode routeMode,
            boolean returnToOffice) {

        AiRoutePlan plan = new AiRoutePlan();
        plan.setOffice(office);
        plan.setManagerEmployee(manager);
        plan.setStatus(AiRoutePlanStatus.DRAFT);
        plan.setPlanCode("AI-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        plan.setOptimizationNote(aiResponse.getMessage());
        plan.setRouteMode(routeMode);
        plan.setReturnToOffice(returnToOffice);
        plan.setOptimizationScope(RouteOptimizationScope.MANAGER_GLOBAL);
        plan.setCreatedByRole("MANAGER");
        plan.setCreatedByEmployeeId(manager.getId());
        plan.setVersionNumber(1);
        plan.setActive(true);

        if (aiResponse.getSummary() != null) {
            AiOptimizationSummaryDto s = aiResponse.getSummary();
            plan.setTotalDistanceKm(toBd(s.getTotalDistanceKm()));
            plan.setTotalDurationMinutes(toBd(s.getTotalDurationMinutes()));
            plan.setTotalFuelCost(toBd(s.getTotalFuelCost()));
            plan.setTotalCod(s.getTotalCod());
            plan.setUnassignedCount(s.getUnassignedOrderCount() != null ? s.getUnassignedOrderCount() : 0);
        }

        // Collect order IDs from AI response
        Set<Integer> orderIds = new HashSet<>();
        for (AiShipperRouteOutputDto routeDto : aiResponse.getRoutes()) {
            if (routeDto.getStops() == null) continue;
            for (AiRouteStopOutputDto stopDto : routeDto.getStops()) {
                if (stopDto.getOrderId() != null) {
                    orderIds.add(stopDto.getOrderId());
                }
            }
        }
        Map<Integer, Order> orderMap = orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        Map<String, Order> orderByTrackingNumber = deliveryOrders.stream()
                .filter(o -> o.getTrackingNumber() != null)
                .collect(Collectors.toMap(
                        Order::getTrackingNumber,
                        Function.identity(),
                        (a, b) -> a
                ));

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
            route.setRouteMode(routeMode);
            route.setReturnToOffice(returnToOffice);
            route.setRouteVersion(1);
            route.setIsActive(true);

            List<AiRoutePlanStop> stops = new ArrayList<>();
            int stopSeq = 1;

            // Business stops (DELIVERY / PICKUP)
            if (routeDto.getStops() != null) {
                for (AiRouteStopOutputDto stopDto : routeDto.getStops()) {
                    // Try orderId first, then fall back to trackingNumber
                    Order order = null;
                    if (stopDto.getOrderId() != null) {
                        order = orderMap.get(stopDto.getOrderId());
                    }
                    if (order == null && stopDto.getTrackingNumber() != null) {
                        order = orderByTrackingNumber.get(stopDto.getTrackingNumber());
                    }
                    if (order == null) {
                        continue;
                    }

                    AiRoutePlanStop stop = new AiRoutePlanStop();
                    stop.setRoute(route);
                    stop.setOrder(order);
                    stop.setStopType(RouteStopType.DELIVERY); // AI mặc định DELIVERY
                    stop.setStopSequence(stopSeq++);
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
                    stop.setStopStatus(RouteStopStatus.PENDING);
                    stop.setIsInserted(false);
                    stops.add(stop);
                }
            }

            // RETURN_TO_OFFICE stop (nếu AI trả về)
            AiRouteStopOutputDto returnStopDto = routeDto.getReturnToOfficeStop();
            if (returnToOffice && returnStopDto != null) {
                AiRoutePlanStop returnStop = new AiRoutePlanStop();
                returnStop.setRoute(route);
                returnStop.setOrder(null); // RETURN_TO_OFFICE không có order
                returnStop.setStopType(RouteStopType.RETURN_TO_OFFICE);
                returnStop.setStopSequence(stopSeq++);
                returnStop.setTrackingNumber(null);
                returnStop.setRecipientName(returnStopDto.getRecipientName() != null ? returnStopDto.getRecipientName() : office.getName());
                returnStop.setRecipientPhone(null);
                returnStop.setRecipientAddress(returnStopDto.getRecipientAddress() != null ? returnStopDto.getRecipientAddress() : office.getName());
                returnStop.setRecipientLatitude(returnStopDto.getLatitude());
                returnStop.setRecipientLongitude(returnStopDto.getLongitude());
                returnStop.setCodAmount(0);
                returnStop.setPriority("NORMAL");
                returnStop.setEtaTime(returnStopDto.getEtaTime());
                returnStop.setEtaMinutesFromStart(returnStopDto.getEtaMinutesFromStart());
                returnStop.setLegDistanceKm(toBd(returnStopDto.getLegDistanceKm()));
                returnStop.setStopStatus(RouteStopStatus.PENDING);
                returnStop.setIsInserted(false);
                stops.add(returnStop);
            }

            route.setStops(stops);
            long businessStopCount = stops.stream().filter(s -> s.getStopType() != RouteStopType.RETURN_TO_OFFICE).count();
            route.setStopCount((int) businessStopCount);
            plan.getRoutes().add(route);
        }

        AiRoutePlan savedPlan = aiRoutePlanRepository.save(plan);

        return savedPlan;
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
                        .routeMode(r.getRouteMode())
                        .returnToOffice(r.getReturnToOffice())
                        .routeVersion(r.getRouteVersion())
                        .isActive(r.getIsActive())
                        .returnToOfficeStop(r.getStops().stream()
                                .filter(s -> s.getStopType() == RouteStopType.RETURN_TO_OFFICE)
                                .findFirst()
                                .map(s -> ManagerAiRouteStopDto.builder()
                                        .stopId(s.getId())
                                        .orderId(null)
                                        .stopType(s.getStopType())
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
                                        .stopStatus(s.getStopStatus())
                                        .isInserted(s.getIsInserted())
                                        .insertedReason(s.getInsertedReason())
                                        .build())
                                .orElse(null))
                        .stops(r.getStops().stream()
                                .sorted(Comparator.comparing(AiRoutePlanStop::getStopSequence))
                                .filter(s -> s.getStopType() != RouteStopType.RETURN_TO_OFFICE)
                                .map(s -> ManagerAiRouteStopDto.builder()
                                        .stopId(s.getId())
                                        .orderId(s.getOrder() != null ? s.getOrder().getId() : null)
                                        .stopType(s.getStopType())
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
                                        .stopStatus(s.getStopStatus())
                                        .isInserted(s.getIsInserted())
                                        .insertedReason(s.getInsertedReason())
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
                .routeMode(plan.getRouteMode())
                .returnToOffice(plan.getReturnToOffice())
                .optimizationScope(plan.getOptimizationScope())
                .versionNumber(plan.getVersionNumber())
                .active(plan.getActive())
                .startedAt(plan.getStartedAt())
                .completedAt(plan.getCompletedAt())
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
                .routeMode(plan.getRouteMode())
                .returnToOffice(plan.getReturnToOffice())
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
