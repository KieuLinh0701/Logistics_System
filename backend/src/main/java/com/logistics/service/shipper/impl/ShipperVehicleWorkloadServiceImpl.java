package com.logistics.service.shipper.impl;

import com.logistics.entity.Employee;
import com.logistics.entity.Order;
import com.logistics.entity.ShipperVehicle;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ShipperVehicleStatus;
import com.logistics.enums.ShipperVehicleType;
import com.logistics.repository.ShipperVehicleRepository;
import com.logistics.service.shipper.ShipperVehicleWorkloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShipperVehicleWorkloadServiceImpl implements ShipperVehicleWorkloadService {

    private final ShipperVehicleRepository shipperVehicleRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void addLoaded(Order order, Employee employee) {
        if (order == null || employee == null) {
            log.warn("[VEHICLE_WORKLOAD_SKIP_NULL] addLoaded order={} employee={}",
                    order == null ? "null" : order.getId(),
                    employee == null ? "null" : employee.getId());
            return;
        }
        ShipperVehicle vehicle = getOrCreateVehicle(employee);
        int currentOrders = vehicle.getCurrentOrders() != null ? vehicle.getCurrentOrders() : 0;
        BigDecimal currentWeight = normalizeWeight(vehicle.getCurrentWeightKg());
        BigDecimal orderWeight = normalizeWeight(order.getWeight());

        currentOrders += 1;
        currentWeight = currentWeight.add(orderWeight);

        vehicle.setCurrentOrders(currentOrders);
        vehicle.setCurrentWeightKg(currentWeight);
        shipperVehicleRepository.save(vehicle);
        shipperVehicleRepository.flush();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void removeLoaded(Order order, Employee employee) {
        if (order == null || employee == null) {
            log.warn("[VEHICLE_WORKLOAD_SKIP_NULL] removeLoaded order={} employee={}",
                    order == null ? "null" : order.getId(),
                    employee == null ? "null" : employee.getId());
            return;
        }
        ShipperVehicle vehicle = getOrCreateVehicle(employee);
        int currentOrders = vehicle.getCurrentOrders() != null ? vehicle.getCurrentOrders() : 0;
        BigDecimal currentWeight = normalizeWeight(vehicle.getCurrentWeightKg());
        BigDecimal orderWeight = normalizeWeight(order.getWeight());

        currentOrders = Math.max(0, currentOrders - 1);
        currentWeight = currentWeight.subtract(orderWeight);
        if (currentWeight.compareTo(BigDecimal.ZERO) < 0) {
            currentWeight = BigDecimal.ZERO;
        }

        vehicle.setCurrentOrders(currentOrders);
        vehicle.setCurrentWeightKg(currentWeight);
        shipperVehicleRepository.save(vehicle);
        shipperVehicleRepository.flush();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void applyTransition(Order order, Employee employee, OrderStatus oldStatus, OrderStatus newStatus) {
        if (order == null || employee == null || newStatus == null) {
            log.warn("[VEHICLE_WORKLOAD_SKIP_NULL] applyTransition order={} employee={} oldStatus={} newStatus={}",
                    order == null ? "null" : order.getId(),
                    employee == null ? "null" : employee.getId(),
                    oldStatus, newStatus);
            return;
        }
        if (oldStatus != null && oldStatus == newStatus) {
            return;
        }
        WorkloadOp op = resolveOp(oldStatus, newStatus);
        if (op == null) {
            return;
        }
        ShipperVehicle vehicle = getOrCreateVehicle(employee);
        int currentOrders = vehicle.getCurrentOrders() != null ? vehicle.getCurrentOrders() : 0;
        BigDecimal currentWeight = normalizeWeight(vehicle.getCurrentWeightKg());
        BigDecimal orderWeight = normalizeWeight(order.getWeight());

        switch (op) {
            case ADD -> {
                currentOrders += 1;
                currentWeight = currentWeight.add(orderWeight);
            }
            case REMOVE -> {
                currentOrders = Math.max(0, currentOrders - 1);
                currentWeight = currentWeight.subtract(orderWeight);
                if (currentWeight.compareTo(BigDecimal.ZERO) < 0) {
                    currentWeight = BigDecimal.ZERO;
                }
            }
        }

        vehicle.setCurrentOrders(currentOrders);
        vehicle.setCurrentWeightKg(currentWeight);
        shipperVehicleRepository.save(vehicle);
        shipperVehicleRepository.flush();
    }

    private WorkloadOp resolveOp(OrderStatus oldStatus, OrderStatus newStatus) {
        if (newStatus == OrderStatus.PICKED_UP) {
            return WorkloadOp.ADD;
        }
        switch (newStatus) {
            case DELIVERED, RETURNED, DELIVERY_FAILED_FINAL, PICKUP_FAILED_FINAL,
                 AT_ORIGIN_OFFICE, CANCELLED -> {
                return WorkloadOp.REMOVE;
            }
            default -> {
            }
        }
        return null;
    }

    private ShipperVehicle getOrCreateVehicle(Employee employee) {
        return shipperVehicleRepository.findByShipperId(employee.getId())
                .orElseGet(() -> {
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
                });
    }

    private BigDecimal normalizeWeight(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private enum WorkloadOp {
        ADD, REMOVE
    }
}