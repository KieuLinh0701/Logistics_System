package com.logistics.service.manager;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.logistics.dto.manager.shipment.ManagerShipmentDetailDto;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.Order;
import com.logistics.entity.Shipment;
import com.logistics.entity.ShipmentOrder;
import com.logistics.entity.ShipperAssignment;
import com.logistics.entity.User;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ShipmentStatus;
import com.logistics.enums.ShipmentType;
import com.logistics.id.ShipmentOrderId;
import com.logistics.mapper.OrderMapper;
import com.logistics.response.BulkResponse;
import com.logistics.response.BulkResponse.BulkResult;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ShipmentRepository;
import com.logistics.repository.ShipperAssignmentRepository;
import com.logistics.utils.OrderUtils;
import com.logistics.utils.ShipmentOrderUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShipmentOrderManagerService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final ShipperAssignmentRepository shipperAssignmentRepository;
    private final EmployeeManagerService employeeManagerService;

    private BulkResult<ManagerShipmentDetailDto> validateOrderForShipment(
            Office userOffice, Shipment shipment, Order order) {

        BulkResult<ManagerShipmentDetailDto> result = new BulkResult<>();
        result.setName(order.getTrackingNumber());

        // Quyền
        if (!((order.getFromOffice() != null && userOffice.getId().equals(order.getFromOffice().getId()))
                || (order.getToOffice() != null && userOffice.getId().equals(order.getToOffice().getId())))) {
            result.setSuccess(false);
            result.setMessage("Bạn không có quyền trên đơn hàng này");
            return result;
        }

        OrderStatus status = order.getStatus();

        // Trạng thái chung
        boolean isValid = ShipmentOrderUtils.isOrderAddableGeneral(order.getStatus())
                && ((shipment.getType() == ShipmentType.DELIVERY
                        && (ShipmentOrderUtils.isOrderAddableIfShipperFromOfficeAssigned(status)
                                || ShipmentOrderUtils.isOrderAddableIfShipperToOfficeAssigned(status)))
                        || (shipment.getType() == ShipmentType.TRANSFER
                                && (ShipmentOrderUtils.isOrderAddableIfDriverFromOfficeAssigned(status)
                                        || ShipmentOrderUtils.isOrderAddableIfDriverToOfficeAssigned(status))));

        if (!isValid) {
            result.setSuccess(false);
            result.setMessage("Trạng thái đơn hàng là '" + OrderUtils.translateOrderStatus(status)
                    + "' không hợp lệ để thêm vào chuyến");
            return result;
        }

        // Kiểm tra trạng thái & vị trí shipper/driver theo loại shipment
        if (shipment.getEmployee() != null && shipment.getEmployee().getUser() != null) {
            Employee emp = shipment.getEmployee();
            boolean validPosition = false;

            if (shipment.getType() == ShipmentType.DELIVERY) {
                if (ShipmentOrderUtils.isOrderAddableIfShipperFromOfficeAssigned(status)
                        && userOffice.getId().equals(order.getFromOffice().getId())) {
                    validPosition = true; // shipper ở fromOffice
                } else if (ShipmentOrderUtils.isOrderAddableIfShipperToOfficeAssigned(status)
                        && userOffice.getId().equals(order.getToOffice().getId())) {
                    validPosition = true; // shipper ở toOffice
                }
            } else if (shipment.getType() == ShipmentType.TRANSFER) {
                if (ShipmentOrderUtils.isOrderAddableIfDriverFromOfficeAssigned(status)
                        && userOffice.getId().equals(order.getFromOffice().getId())) {
                    validPosition = true; // driver ở fromOffice
                } else if (ShipmentOrderUtils.isOrderAddableIfDriverToOfficeAssigned(status)
                        && userOffice.getId().equals(order.getToOffice().getId())) {
                    validPosition = true; // driver ở toOffice
                }
            }

            if (!validPosition) {
                result.setSuccess(false);

                String employeeType = shipment.getType() == ShipmentType.DELIVERY ? "Nhân viên giao hàng" : "Tài xế";
                String employeePosition;
                if (userOffice.getId().equals(order.getFromOffice().getId())) {
                    employeePosition = "tại nơi gửi";
                } else if (userOffice.getId().equals(order.getToOffice().getId())) {
                    employeePosition = "tại nơi nhận";
                } else {
                    employeePosition = "ở vị trí không xác định";
                }

                result.setMessage(String.format(
                        "%s %s không thể thêm đơn hàng đang ở trạng thái '%s'",
                        employeeType, employeePosition, OrderUtils.translateOrderStatus(status)));
                return result;
            }

            // Kiểm tra phân công khu vực cho shipper (chỉ DELIVERY)
            if (shipment.getType() == ShipmentType.DELIVERY) {
                LocalDateTime now = LocalDateTime.now();

                // Xác định khu vực cần check:
                Integer targetCityCode;
                Integer targetWardCode;

                if (userOffice.getId().equals(order.getFromOffice().getId())) {
                    targetCityCode = order.getSenderCityCode();
                    targetWardCode = order.getSenderWardCode();
                } else if (userOffice.getId().equals(order.getToOffice().getId())) {
                    targetCityCode = order.getRecipientAddress().getCityCode();
                    targetWardCode = order.getRecipientAddress().getWardCode();
                } else {
                    result.setSuccess(false);
                    result.setMessage("Vị trí phân công của nhân viên không phù hợp với đơn hàng");
                    return result;
                }

                boolean hasActiveAssignment = shipperAssignmentRepository
                        .findActiveAssignments(emp.getUser().getId(), now)
                        .stream()
                        .anyMatch(sa -> sa.getCityCode().equals(targetCityCode)
                                && sa.getWardCode().equals(targetWardCode));

                if (!hasActiveAssignment) {
                    result.setSuccess(false);
                    result.setMessage("Không có phân công khu vực hợp lệ cho Nhân viên giao hàng tại khu vực này");
                    return result;
                }
            }
        }

        // Trùng trong shipment hiện tại
        boolean existsInCurrent = shipment.getShipmentOrders().stream()
                .anyMatch(so -> so.getOrder().getId().equals(order.getId()));
        if (existsInCurrent) {
            result.setSuccess(false);
            result.setMessage("Đơn hàng đã tồn tại trong chuyến này");
            return result;
        }

        // Trùng trong shipment khác
        boolean existsInOther = orderRepository.existsByIdAndShipmentStatusIn(order.getId(),
                List.of(ShipmentStatus.PENDING, ShipmentStatus.IN_TRANSIT));
        if (existsInOther) {
            result.setSuccess(false);
            result.setMessage(
                    "Đơn hàng đang nằm trong chuyến khác chuẩn bị vận chuyển hoặc đang trên đường vận chuyển");
            return result;
        }

        result.setSuccess(true);
        result.setMessage("Đơn hàng hợp lệ");
        result.setResult(OrderMapper.toManagerShipmentDetailDto(order));
        return result;
    }

    private boolean isOverload(Shipment shipment, BigDecimal totalWeight, Order order) {
        if (shipment.getVehicle() == null)
            return false;
        return totalWeight.add(order.getWeight()).compareTo(shipment.getVehicle().getCapacity()) > 0;
    }

    public BulkResponse<ManagerShipmentDetailDto> checkOrdersForShipment(Integer userId, Integer shipmentId,
            List<String> trackingNumbers) {

        List<BulkResult<ManagerShipmentDetailDto>> results = new ArrayList<>();
        int totalValid = 0, totalInvalid = 0;
        Set<String> processed = new HashSet<>();

        try {
            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);
            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .filter(s -> s.getFromOffice() != null && s.getFromOffice().getId().equals(userOffice.getId()))
                    .orElseThrow(() -> new RuntimeException(
                            "Chuyến hàng không tồn tại hoặc bạn không có quyền"));

            BigDecimal totalWeight = shipment.getShipmentOrders().stream()
                    .map(so -> so.getOrder().getWeight())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            for (String tn : trackingNumbers) {
                if (processed.contains(tn)) {
                    BulkResult<ManagerShipmentDetailDto> r = new BulkResult<>();
                    r.setName(tn);
                    r.setSuccess(false);
                    r.setMessage("Đã tồn tại đơn hàng này trong yêu cầu được xử lý trước đó");
                    results.add(r);
                    totalInvalid++;
                    continue;
                }

                processed.add(tn);

                Order order = orderRepository.findByTrackingNumber(tn).orElse(null);
                if (order == null) {
                    BulkResult<ManagerShipmentDetailDto> r = new BulkResult<>();
                    r.setName(tn);
                    r.setSuccess(false);
                    r.setMessage("Đơn hàng không tồn tại");
                    results.add(r);
                    totalInvalid++;
                    continue;
                }

                BulkResult<ManagerShipmentDetailDto> result = validateOrderForShipment(userOffice, shipment, order);
                results.add(result);
                if (result.isSuccess()) {
                    totalValid++;
                    totalWeight = totalWeight.add(order.getWeight());
                } else {
                    totalInvalid++;
                }
            }

            return new BulkResponse<>(totalInvalid == 0,
                    totalInvalid == 0 ? "Tất cả đơn hợp lệ" : "Một số đơn không hợp lệ",
                    totalValid, totalInvalid, results);

        } catch (Exception e) {
            results.clear();
            for (String tn : trackingNumbers) {
                BulkResult<ManagerShipmentDetailDto> r = new BulkResult<>();
                r.setName(tn);
                r.setSuccess(false);
                r.setMessage(e.getMessage());
                results.add(r);
            }
            return new BulkResponse<>(false, "Lỗi: " + e.getMessage(), 0, trackingNumbers.size(), results);
        }
    }

    // Thêm nhiều Order vào Shipment
    public BulkResponse<String> saveShipmentOrders(Integer userId, Integer shipmentId,
            List<Integer> removedOrderIds,
            List<Integer> addedOrderIds) {

        List<BulkResult<String>> results = new ArrayList<>();
        int totalImported = 0, totalFailed = 0;

        try {
            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);
            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .filter(s -> s.getFromOffice() != null && s.getFromOffice().getId().equals(userOffice.getId()))
                    .orElseThrow(() -> new RuntimeException(
                            "Chuyến hàng không tồn tại hoặc bạn không có quyền"));

            // Xóa các đơn nếu có
            if (removedOrderIds != null && !removedOrderIds.isEmpty()) {
                shipment.getShipmentOrders().removeIf(so -> removedOrderIds.contains(so.getOrder().getId()));
            }

            // Tính tổng trọng lượng hiện tại
            BigDecimal totalWeight = shipment.getShipmentOrders().stream()
                    .map(so -> so.getOrder().getWeight())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<Order> ordersToAdd = new ArrayList<>();

            if (addedOrderIds != null && !addedOrderIds.isEmpty()) {
                for (int i = 0; i < addedOrderIds.size(); i++) {
                    Integer orderId = addedOrderIds.get(i);
                    BulkResult<String> result = new BulkResult<>();

                    Order order = orderRepository.findById(orderId).orElse(null);
                    if (order == null) {
                        result.setName(String.valueOf(orderId));
                        result.setSuccess(false);
                        result.setMessage("Đơn hàng không tồn tại");
                        result.setResult(String.valueOf(orderId));
                        results.add(result);
                        totalFailed++;
                        continue;
                    }

                    result.setName(order.getTrackingNumber());
                    result.setResult(order.getTrackingNumber());

                    // Kiểm tra hợp lệ
                    BulkResult<ManagerShipmentDetailDto> checkResult = validateOrderForShipment(userOffice, shipment,
                            order);
                    if (!checkResult.isSuccess()) {
                        result.setSuccess(false);
                        result.setMessage(checkResult.getMessage());
                        results.add(result);
                        totalFailed++;
                        continue;
                    }

                    if (isOverload(shipment, totalWeight, order)) {
                        // Tạo tracking list các đơn chưa xét
                        List<String> remainingTrackingNumbers = new ArrayList<>();
                        for (int j = i; j < addedOrderIds.size(); j++) {
                            Order o = orderRepository.findById(addedOrderIds.get(j)).orElse(null);
                            if (o != null)
                                remainingTrackingNumbers.add(o.getTrackingNumber());
                        }

                        BulkResult<String> overloadResult = new BulkResult<>();
                        overloadResult.setName(order.getTrackingNumber());
                        overloadResult.setSuccess(false);
                        String message = "Đơn hàng gây quá tải.";
                        if (remainingTrackingNumbers != null && !remainingTrackingNumbers.isEmpty()) {
                            message += "Các đơn chưa xét: " +
                                    remainingTrackingNumbers.stream()
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.joining(", "));
                        }
                        overloadResult.setMessage(message);
                        results.add(overloadResult); 
                        totalFailed++;
                        break;
                    }

                    ordersToAdd.add(order);
                    result.setSuccess(true);
                    result.setMessage("Thêm thành công");
                    results.add(result);
                    totalWeight = totalWeight.add(order.getWeight());
                    totalImported++;
                }

                // Thêm đơn vào shipment
                for (Order order : ordersToAdd) {
                    ShipmentOrder shipmentOrder = new ShipmentOrder();

                    shipmentOrder.setShipment(shipment);
                    shipmentOrder.setOrder(order);

                    ShipmentOrderId id = new ShipmentOrderId();
                    id.setShipmentId(shipment.getId());
                    id.setOrderId(order.getId());
                    shipmentOrder.setId(id);

                    shipment.getShipmentOrders().add(shipmentOrder);
                }
            }

            shipmentRepository.save(shipment);

            return new BulkResponse<>(totalFailed == 0,
                    totalFailed == 0 ? "Cập nhật chuyến hàng thành công" : "Một số đơn không thể thêm",
                    totalImported, totalFailed, results);

        } catch (Exception e) {
            results.clear();
            if (addedOrderIds != null) {
                for (Integer orderId : addedOrderIds) {
                    BulkResult<String> r = new BulkResult<>();
                    r.setName(String.valueOf(orderId));
                    r.setSuccess(false);
                    r.setMessage(e.getMessage());
                    r.setResult(String.valueOf(orderId));
                    results.add(r);
                }
            }
            return new BulkResponse<>(false, "Lỗi: " + e.getMessage(), 0,
                    addedOrderIds != null ? addedOrderIds.size() : 0, results);
        }
    }
}