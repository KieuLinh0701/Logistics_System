package com.logistics.service.manager;

import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.dto.manager.order.ManagerOrderListDto;
import com.logistics.dto.manager.order.ManagerOrderStatusCountResponse;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.CommonErrorCode;
import com.logistics.exception.enums.OrderErrorCode;
import com.logistics.exception.enums.ServiceTypeErrorCode;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.OrderPrintMapper;
import com.logistics.repository.*;
import com.logistics.request.manager.order.ManagerOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.FeePublicService;
import com.logistics.service.common.NotificationService;
import com.logistics.service.user.*;
import com.logistics.specification.OrderSpecification;
import com.logistics.utils.AddressUtils;
import com.logistics.utils.ManagerOrderEditRuleUtils;
import com.logistics.utils.OrderFieldUtils;
import com.logistics.utils.OrderUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.logistics.utils.OrderUtils.*;

@Service
@RequiredArgsConstructor
public class OrderManagerService {

    private final OrderRepository repository;

    private final OrderProductRepository orderProductRepository;

    private final OrderHistoryRepository orderHistoryRepository;

    private final PromotionUserService promotionUserService;

    private final OfficeRepository officeRepository;

    private final AddressUserService addressUserService;

    private final ServiceTypeUserService serviceTypeUserService;

    private final FeePublicService feeService;

    private final ProductUserService productUserService;

    private final OrderHistoryUserService orderHistoryUserService;

    private final EmployeeManagerService employeeManagerService;

    private final NotificationService notificationService;

    private final PickupAttemptRepository pickupAttemptRepository;

    public ListResponse<ManagerOrderListDto> list(int userId, UserOrderSearchRequest request) {
        int page = request.getPage();
        int limit = request.getLimit();
        String search = request.getSearch();
        String payer = request.getPayer();
        String status = request.getStatus();
        String pickupType = request.getPickupType();
        Integer serviceTypeId = request.getServiceTypeId();
        String paymentStatus = request.getPaymentStatus();
        String cod = request.getCod();
        String sort = request.getSort();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate()
                .isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;

        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate()
                .isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                .and(OrderSpecification.officeId(userOffice.getId()))
                .and(OrderSpecification.excludeDraft())
                .and(OrderSpecification.searchManager(search))
                .and(OrderSpecification.payer(payer))
                .and(OrderSpecification.status(status))
                .and(OrderSpecification.pickupType(pickupType))
                .and(OrderSpecification.serviceTypeId(serviceTypeId))
                .and(OrderSpecification.paymentStatus(paymentStatus))
                .and(OrderSpecification.cod(cod))
                .and(OrderSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt")
                    .descending();
            case "oldest" -> Sort.by("createdAt")
                    .ascending();
            case "cod_high" -> Sort.by("cod")
                    .descending();
            case "cod_low" -> Sort.by("cod")
                    .ascending();
            case "order_value_high" -> Sort.by("orderValue")
                    .descending();
            case "order_value_low" -> Sort.by("orderValue")
                    .ascending();
            case "fee_high" -> Sort.by("totalFee")
                    .descending();
            case "fee_low" -> Sort.by("totalFee")
                    .ascending();
            case "weight_high" -> Sort.by("weight")
                    .descending();
            case "weight_low" -> Sort.by("weight")
                    .ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
        Page<Order> pageData = repository.findAll(spec, pageable);

        List<ManagerOrderListDto> list = pageData.getContent()
                .stream()
                .map(OrderMapper::toManagerOrderListDto)
                .toList();

        int total = (int) pageData.getTotalElements();

        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<ManagerOrderListDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    public List<ManagerOrderStatusCountResponse> getStatusCounts(Integer userId) {
        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        List<Object[]> raw = repository.countByStatusForOffice(userOffice.getId());

        List<ManagerOrderStatusCountResponse> counts = raw.stream()
                .map(row -> new ManagerOrderStatusCountResponse(
                        (String) row[0],
                        (long) row[1]
                ))
                .collect(Collectors.toList());

        long total = counts.stream()
                .mapToLong(ManagerOrderStatusCountResponse::getCount)
                .sum();
        counts.add(0, new ManagerOrderStatusCountResponse("ALL", total));

        return counts;
    }

    public List<Integer> getAllOrderIds(int userId, UserOrderSearchRequest request) {
        String search = request.getSearch();
        String payer = request.getPayer();
        String status = request.getStatus();
        String pickupType = request.getPickupType();
        Integer serviceTypeId = request.getServiceTypeId();
        String paymentStatus = request.getPaymentStatus();
        String cod = request.getCod();
        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate()
                .isBlank()
                ? LocalDateTime.parse(request.getStartDate())
                : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate()
                .isBlank()
                ? LocalDateTime.parse(request.getEndDate())
                : null;

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                .and(OrderSpecification.officeId(userOffice.getId()))
                .and(OrderSpecification.excludeDraft())
                .and(OrderSpecification.searchManager(search))
                .and(OrderSpecification.payer(payer))
                .and(OrderSpecification.status(status))
                .and(OrderSpecification.pickupType(pickupType))
                .and(OrderSpecification.serviceTypeId(serviceTypeId))
                .and(OrderSpecification.paymentStatus(paymentStatus))
                .and(OrderSpecification.cod(cod))
                .and(OrderSpecification.createdAtBetween(startDate, endDate));

        List<Order> orders = repository.findAll(spec);

        return orders.stream()
                .filter(order -> order.getTrackingNumber() != null)
                .map(Order::getId)
                .toList();
    }

    public ManagerOrderDetailDto getOrderByTrackingNumber(int userId, String trackingNumber) {
        Order order = repository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        boolean hasAccess = (order.getFromOffice() != null
                && userOffice.getId()
                .equals(order.getFromOffice()
                        .getId()))
                || (order.getToOffice() != null && userOffice.getId()
                .equals(order.getToOffice()
                        .getId()));

        if (!hasAccess) {
            throw new AppException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        List<OrderHistory> orderHistories = orderHistoryRepository
                .findByOrderIdOrderByActionTimeDesc(order.getId());

        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        var pickupAttempts = pickupAttemptRepository.findByOrderIdOrderByAttemptedAtDesc(order.getId());

        return OrderMapper.toManagerOrderDetailDto(order, orderHistories, orderProducts, pickupAttempts);
    }

    @Transactional
    public List<OrderPrintDto> getOrdersForPrint(
            Integer userId,
            List<Integer> orderIds) {
            List<Order> orders = repository.findByIdIn(orderIds);

            if (orders.isEmpty()) {
                throw new AppException(OrderErrorCode.ORDERS_NOT_FOUND_TO_PRINT);
            }

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            List<Order> printableOrders = orders.stream()
                    .filter(order -> OrderUtils.canManagerPrint(order.getStatus()))
                    .filter(order -> {
                        boolean fromMatch = order.getFromOffice() != null
                                && userOffice.getId()
                                .equals(order.getFromOffice()
                                        .getId());
                        boolean toMatch = order.getToOffice() != null
                                && userOffice.getId()
                                .equals(order.getToOffice()
                                        .getId());
                        return fromMatch || toMatch;
                    })
                    .toList();

            if (printableOrders.isEmpty()) {
                throw new AppException(OrderErrorCode.ORDERS_INELIGIBLE_FOR_PRINT);
            }

            return printableOrders.stream()
                    .map(order -> {
                        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
                        return OrderPrintMapper.toDto(order, orderProducts);
                    })
                    .toList();
    }

    public void cancelOrder(Integer userId, Integer orderId) {
        Order order = getOrderById(orderId);

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (!OrderUtils.canManagerCancel(order.getStatus(), order.getCreatedByType())) {
            throw new AppException(OrderErrorCode.ORDER_CANNOT_CANCEL);
        }

        if (order.getFromOffice() == null || !userOffice.getId()
                .equals(order.getFromOffice()
                        .getId())) {
            throw new AppException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        repository.save(order);

        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        productUserService.restoreStockFromOrder(orderProducts);

        if (order.getPromotion() != null) {
            promotionUserService.decreaseUsage(order.getPromotion()
                    .getId(), userId);
        }

        if (!order.getCreatedByType()
                .equals(OrderCreatorType.USER) && order.getPayer()
                .equals(OrderPayerType.SHOP)) {
            order.setPaymentStatus(OrderPaymentStatus.REFUNDED);
        }

        orderHistoryUserService.save(
                order,
                null,
                null,
                null,
                OrderHistoryActionType.CANCELLED,
                null);

        if (order.getUser() != null) {
            notificationService.create(
                    "Đơn hàng của bạn đã bị hủy",
                    String.format(
                            "Đơn hàng có mã vận đơn #%s của bạn đã bị hủy bởi nhân viên chúng tôi. Nếu bạn không yêu cầu hành động này, vui lòng liên hệ để được hỗ trợ.",
                            order.getTrackingNumber()),
                    "order",
                    order.getUser()
                            .getId(),
                    null,
                    "orders/list",
                    order.getTrackingNumber());
        }
    }

    public void confirmOrder(Integer userId, Integer orderId) {
        Order order = getOrderById(orderId);

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (!OrderUtils.canManagerConfirm(order.getStatus(), order.getPickupType())) {
            throw new AppException(OrderErrorCode.ORDER_CANNOT_CONFIRM);
        }

        if (order.getFromOffice() == null || !userOffice.getId()
                .equals(order.getFromOffice()
                        .getId())) {
            throw new AppException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        repository.save(order);

        if (order.getUser() != null) {
            notificationService.create(
                    "Đơn hàng đã được xác nhận",
                    String.format(
                            "Đơn hàng #%s đã được xác nhận. Vui lòng chuẩn bị hàng hóa và mang đến bưu cục để hoàn tất việc gửi hàng.",
                            order.getTrackingNumber()),
                    "order",
                    order.getUser()
                            .getId(),
                    null,
                    "orders/list",
                    order.getTrackingNumber());
        }
    }

    public String create(Integer userId, ManagerOrderCreateRequest request) {
            validateCreate(request);

            validateWeight(
                    request.getWeight(),
                    request.getOriginalWeight(),
                    request.getHeight(),
                    request.getLength(),
                    request.getWidth());

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            Integer totalFee = 0;
            if (userOffice.getCityCode() != null) {
                totalFee = feeService.calculateTotalFeeManager(
                        request.getWeight(),
                        request.getServiceTypeId(),
                        userOffice.getCityCode(),
                        request.getRecipientCityCode(),
                        request.getOrderValue(),
                        0);
            }

            Employee currentEmployee = userOffice.getManager();

            ServiceType serviceType = serviceTypeUserService.findById(request.getServiceTypeId())
                    .orElseThrow(() -> new AppException(ServiceTypeErrorCode.SERVICE_TYPE_NOT_FOUND));

            Integer shippingFee = feeService.calculateShippingFee(
                    request.getWeight(),
                    request.getServiceTypeId(),
                    userOffice.getCityCode(),
                    request.getRecipientCityCode());

            Order order = new Order();
            order.setStatus(OrderStatus.AT_ORIGIN_OFFICE);
            order.setTrackingNumber(generateUniqueTrackingNumber(order.getStatus()));
            order.setCreatedByType(OrderCreatorType.MANAGER);

            order.setSenderName(request.getSenderName());
            order.setSenderPhone(request.getSenderPhone());
            order.setSenderCityCode(request.getSenderCityCode());
            order.setSenderCityName(request.getSenderCityName());
            order.setSenderWardCode(request.getSenderWardCode());
            order.setSenderWardName(request.getSenderWardName());
            order.setSenderDetail(request.getSenderDetail());
            order.setSenderLatitude(request.getSenderLatitude());
            order.setSenderLongitude(request.getSenderLongitude());
            order.setSenderFullAddress(
                    AddressUtils.buildFullAddress(
                            request.getSenderDetail(),
                            request.getSenderWardName(),
                            request.getSenderCityName()
                    )
            );

            order.setRecipientName(request.getRecipientName());
            order.setRecipientPhone(request.getRecipientPhone());
            order.setRecipientCityCode(request.getRecipientCityCode());
            order.setRecipientCityName(request.getRecipientCityName());
            order.setRecipientWardCode(request.getRecipientWardCode());
            order.setRecipientWardName(request.getRecipientWardName());
            order.setRecipientDetail(request.getRecipientDetail());
            order.setRecipientLatitude(request.getRecipientLatitude());
            order.setRecipientLongitude(request.getRecipientLongitude());
            order.setRecipientFullAddress(
                    AddressUtils.buildFullAddress(
                            request.getRecipientDetail(),
                            request.getRecipientWardName(),
                            request.getRecipientCityName()
                    )
            );

            order.setPickupType(OrderPickupType.AT_OFFICE);
            order.setWeight(request.getWeight());
            order.setOriginalWeight(request.getWeight());
            order.setHeight(request.getHeight());
            order.setWidth(request.getWidth());
            order.setLength(request.getLength());
            order.setServiceType(serviceType);
            order.setShippingFee(shippingFee);
            order.setCod(0);
            order.setDiscountAmount(0);
            order.setOrderValue(request.getOrderValue());
            order.setTotalFee(totalFee);
            order.setPayer(OrderPayerType.valueOf(request.getPayer()));
            order.setPaymentStatus(OrderPaymentStatus.UNPAID);
            order.setNotes(request.getNotes());
            order.setFromOffice(userOffice);
            order.setEmployee(currentEmployee);
            order.setCodStatus(OrderCodStatus.NONE);
            if (OrderPayerType.valueOf(request.getPayer())
                    .equals(OrderPayerType.SHOP)) {
                order.setPaymentStatus(OrderPaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
            }

            Order newOrder = repository.save(order);

            orderHistoryUserService.save(newOrder, null, userOffice,
                    null, OrderHistoryActionType.IMPORTED, null);

            return newOrder.getTrackingNumber();
    }

    @Transactional
    public void update(Integer userId, Integer orderId, ManagerOrderCreateRequest request) {
            validateCreate(request);

            validateWeight(
                    request.getWeight(),
                    request.getOriginalWeight(),
                    request.getHeight(),
                    request.getLength(),
                    request.getWidth());

            Order order = getOrderById(orderId);

            BigDecimal oldWeight = order.getWeight();

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            if (!ManagerOrderEditRuleUtils.canEditManagerOrder(order.getStatus(), order.getCreatedByType())) {
                throw new AppException(OrderErrorCode.ORDER_CANNOT_EDIT);
            }

            if (!((order.getFromOffice() != null && userOffice.getId()
                    .equals(order.getFromOffice()
                            .getId()))
                    || (order.getToOffice() != null && userOffice.getId()
                    .equals(order.getToOffice()
                            .getId())))) {
                throw new AppException(OrderErrorCode.ORDER_ACCESS_DENIED);
            }

            OrderStatus currentStatus = order.getStatus();
            OrderCreatorType creatorType = order.getCreatedByType();

            // NGƯỜI GỬI
            updateManagerField("senderName",
                    order.getSenderName(),
                    request.getSenderName(),
                    currentStatus, creatorType,
                    order::setSenderName);

            updateManagerField("senderPhoneNumber",
                    order.getSenderPhone(),
                    request.getSenderPhone(),
                    currentStatus, creatorType,
                    order::setSenderPhone);

            updateManagerField("senderCityCode",
                    order.getSenderCityCode(),
                    request.getSenderCityCode(),
                    currentStatus, creatorType,
                    order::setSenderCityCode);

            updateManagerField("senderCityName",
                    order.getSenderCityName(),
                    request.getSenderCityName(),
                    currentStatus, creatorType,
                    order::setSenderCityName);

            updateManagerField("senderWardCode",
                    order.getSenderWardCode(),
                    request.getSenderWardCode(),
                    currentStatus, creatorType,
                    order::setSenderWardCode);

            updateManagerField("senderWardName",
                    order.getSenderWardName(),
                    request.getSenderWardName(),
                    currentStatus, creatorType,
                    order::setSenderWardName);

            updateManagerField("senderDetailAddress",
                    order.getSenderDetail(),
                    request.getSenderDetail(),
                    currentStatus, creatorType,
                    order::setSenderDetail);

            updateManagerField("senderLatitude",
                    order.getSenderLatitude(),
                    request.getSenderLatitude(),
                    currentStatus, creatorType,
                    order::setSenderLatitude);

            updateManagerField("senderLongitude",
                    order.getSenderLongitude(),
                    request.getSenderLongitude(),
                    currentStatus, creatorType,
                    order::setSenderLongitude);

            order.setSenderFullAddress(AddressUtils.buildFullAddress(
                    order.getSenderDetail(),
                    order.getSenderWardName(),
                    order.getSenderCityName()
            ));

            // NGƯỜI NHẬN
            updateManagerField("recipientName",
                    order.getRecipientName(),
                    request.getRecipientName(),
                    currentStatus, creatorType,
                    order::setRecipientName);

            updateManagerField("recipientPhoneNumber",
                    order.getRecipientPhone(),
                    request.getRecipientPhone(),
                    currentStatus, creatorType,
                    order::setRecipientPhone);

            updateManagerField("recipientCityCode",
                    order.getRecipientCityCode(),
                    request.getRecipientCityCode(),
                    currentStatus, creatorType,
                    order::setRecipientCityCode);

            updateManagerField("recipientCityName",
                    order.getRecipientCityName(),
                    request.getRecipientCityName(),
                    currentStatus, creatorType,
                    order::setRecipientCityName);

            updateManagerField("recipientWardCode",
                    order.getRecipientWardCode(),
                    request.getRecipientWardCode(),
                    currentStatus, creatorType,
                    order::setRecipientWardCode);

            updateManagerField("recipientWardName",
                    order.getRecipientWardName(),
                    request.getRecipientWardName(),
                    currentStatus, creatorType,
                    order::setRecipientWardName);

            updateManagerField("recipientDetailAddress",
                    order.getRecipientDetail(),
                    request.getRecipientDetail(),
                    currentStatus, creatorType,
                    order::setRecipientDetail);

            updateManagerField("recipientLatitude",
                    order.getRecipientLatitude(),
                    request.getRecipientLatitude(),
                    currentStatus, creatorType,
                    order::setRecipientLatitude);

            updateManagerField("recipientLongitude",
                    order.getRecipientLongitude(),
                    request.getRecipientLongitude(),
                    currentStatus,
                    creatorType,
                    order::setRecipientLongitude);

            order.setRecipientFullAddress(AddressUtils.buildFullAddress(
                    order.getRecipientDetail(),
                    order.getRecipientWardName(),
                    order.getRecipientCityName()
            ));

            if (request.getWeight() != null) {

                if (order.getCreatedByType()
                        .equals(OrderCreatorType.USER)) {
                    updateManagerField(
                            "originalWeight",
                            order.getOriginalWeight(),
                            request.getOriginalWeight(),
                            currentStatus,
                            creatorType,
                            order::setAdjustedOriginalWeight);
                    updateManagerField(
                            "height",
                            order.getHeight(),
                            request.getHeight(),
                            currentStatus,
                            creatorType,
                            order::setAdjustedHeight);
                    updateManagerField(
                            "length",
                            order.getLength(),
                            request.getLength(),
                            currentStatus,
                            creatorType,
                            order::setAdjustedLength);
                    updateManagerField(
                            "width",
                            order.getWidth(),
                            request.getWidth(),
                            currentStatus,
                            creatorType,
                            order::setAdjustedWidth);
                    updateManagerField(
                            "weight",
                            order.getWeight(),
                            request.getWeight(),
                            currentStatus,
                            creatorType,
                            order::setAdjustedWeight);
                } else {
                    updateManagerField(
                            "originalWeight",
                            order.getOriginalWeight(),
                            request.getOriginalWeight(),
                            currentStatus,
                            creatorType,
                            order::setOriginalWeight);
                    updateManagerField(
                            "weight",
                            order.getWeight(),
                            request.getWeight(),
                            currentStatus,
                            creatorType,
                            order::setWeight);
                    updateManagerField(
                            "height",
                            order.getHeight(),
                            request.getHeight(),
                            currentStatus,
                            creatorType,
                            order::setHeight);
                    updateManagerField(
                            "length",
                            order.getLength(),
                            request.getLength(),
                            currentStatus,
                            creatorType,
                            order::setLength);
                    updateManagerField(
                            "width",
                            order.getWidth(),
                            request.getWidth(),
                            currentStatus,
                            creatorType,
                            order::setWidth);
                }
            }

            if (order.getUser() != null && request.getWeight() != null) {

                if (oldWeight == null || oldWeight.compareTo(request.getWeight()) != 0) {

                    int calcShippingFee = feeService.calculateShippingFee(
                            request.getWeight(),
                            order.getServiceType()
                                    .getId(),
                            order.getSenderCityCode(),
                            order.getRecipientCityCode());
                    order.setShippingFee(calcShippingFee);

                    int calcServiceFee = feeService.calculateTotalFee(
                            request.getWeight(),
                            order.getServiceType()
                                    .getId(),
                            order.getSenderCityCode(),
                            order.getRecipientCityCode(),
                            order.getOrderValue(),
                            order.getCod());

                    int discountAmount = 0;

                    if (order.getPromotion() != null) {
                        promotionUserService.decreaseUsage(order.getPromotion()
                                .getId(), userId);

                        order.setPromotion(null);
                    }

                    int calcTotalFee = calcServiceFee - discountAmount;
                    order.setDiscountAmount(discountAmount);
                    order.setTotalFee(calcTotalFee);

                    notificationService.create(
                            "Điều chỉnh khối lượng đơn hàng",
                            String.format(
                                    "Đơn hàng với mã vận đơn #%s phát hiện sai lệch khối lượng so với khai báo ban đầu. "
                                            +
                                            "Hệ thống đã tự động điều chỉnh từ %s kg thành %s kg, phí vận chuyển được cập nhật và khuyến mãi đã bị hủy.",
                                    order.getTrackingNumber(),
                                    oldWeight == null ? "0" : oldWeight.toPlainString(),
                                    request.getWeight()
                                            .toPlainString()),
                            "order",
                            order.getUser()
                                    .getId(),
                            null,
                            "orders/tracking",
                            order.getTrackingNumber());
                }
            }

            // NOTES
            updateManagerField("notes",
                    order.getNotes(),
                    request.getNotes(),
                    currentStatus, creatorType,
                    order::setNotes);

            repository.save(order);
    }

    public byte[] export(Integer userId, UserOrderSearchRequest request) {
        String search = request.getSearch();
        String payer = request.getPayer();
        String status = request.getStatus();
        String pickupType = request.getPickupType();
        Integer serviceTypeId = request.getServiceTypeId();
        String paymentStatus = request.getPaymentStatus();
        String cod = request.getCod();
        String sort = request.getSort();

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                .and(OrderSpecification.officeId(userOffice.getId()))
                .and(OrderSpecification.excludeDraft())
                .and(OrderSpecification.searchManager(search))
                .and(OrderSpecification.payer(payer))
                .and(OrderSpecification.status(status))
                .and(OrderSpecification.pickupType(pickupType))
                .and(OrderSpecification.serviceTypeId(serviceTypeId))
                .and(OrderSpecification.paymentStatus(paymentStatus))
                .and(OrderSpecification.cod(cod))
                .and(OrderSpecification.createdAtBetween(startDate, endDate));

        Sort sortOpt = sort != null ? switch (sort.toLowerCase()) {
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            case "cod_high" -> Sort.by("cod").descending();
            case "cod_low" -> Sort.by("cod").ascending();
            case "order_value_high" -> Sort.by("orderValue").descending();
            case "order_value_low" -> Sort.by("orderValue").ascending();
            case "fee_high" -> Sort.by("totalFee").descending();
            case "fee_low" -> Sort.by("totalFee").ascending();
            case "weight_high" -> Sort.by("weight").descending();
            case "weight_low" -> Sort.by("weight").ascending();
            default -> Sort.by("createdAt").descending();
        } : Sort.by("createdAt").descending();

        List<Order> orders = repository.findAll(spec, sortOpt);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Orders");

            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont font = (XSSFFont) workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, null));
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(
                    new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Mã đơn",
                    "Trạng thái",
                    "Tên người gửi", "SĐT người gửi", "Địa chỉ người gửi",
                    "Tên người nhận", "SĐT người nhận", "Địa chỉ người nhận",
                    "Giá trị đơn", "COD (chưa phí)", "Phí dịch vụ",
                    "Hình thức lấy hàng", "Dịch vụ giao hàng",
                    "Người thanh toán", "Trạng thái thanh toán",
                    "Khối lượng (Kg)", "Khối lượng điều chỉnh (Kg)",
                    "Người tạo đơn", "Mã nhân viên / người dùng",
                    "Thời gian tạo đơn", "Thời gian giao hàng", "Thời gian thanh toán"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

            int rowIdx = 1;
            for (Order o : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(o.getTrackingNumber() != null ? o.getTrackingNumber() : "Chưa có mã");
                row.createCell(1).setCellValue(OrderUtils.translateOrderStatus(o.getStatus()));

                // Người gửi
                row.createCell(2).setCellValue(o.getSenderName() != null ? o.getSenderName() : "");
                row.createCell(3).setCellValue(o.getSenderPhone() != null ? o.getSenderPhone() : "");
                row.createCell(4).setCellValue(o.getSenderFullAddress() != null ? o.getSenderFullAddress() : "");

                // Người nhận
                row.createCell(5).setCellValue(o.getRecipientName() != null ? o.getRecipientName() : "");
                row.createCell(6).setCellValue(o.getRecipientPhone() != null ? o.getRecipientPhone() : "");
                row.createCell(7).setCellValue(o.getRecipientFullAddress() != null ? o.getRecipientFullAddress() : "");

                // Tiền
                row.createCell(8).setCellValue(o.getOrderValue() != null ? o.getOrderValue().doubleValue() : 0);
                row.createCell(9).setCellValue(o.getCod() != null ? o.getCod().doubleValue() : 0);
                row.createCell(10).setCellValue(o.getTotalFee() != null ? o.getTotalFee().doubleValue() : 0);

                // Giao hàng
                row.createCell(11).setCellValue(translateOrderPickupType(o.getPickupType()));
                row.createCell(12).setCellValue(
                        o.getServiceType() != null && o.getServiceType().getName() != null
                                ? o.getServiceType().getName()
                                : "");

                // Thanh toán
                row.createCell(13).setCellValue(translateOrderPayerType(o.getPayer()));
                row.createCell(14).setCellValue(translateOrderPaymentStatus(o.getPaymentStatus()));

                // Khối lượng
                row.createCell(15).setCellValue(o.getWeight() != null ? o.getWeight().doubleValue() : 0);
                row.createCell(16).setCellValue(o.getAdjustedWeight() != null ? o.getAdjustedWeight().doubleValue() : 0);

                // Người tạo
                row.createCell(17).setCellValue(translateOrderCreatorType(o.getCreatedByType()));
                String creatorCode = (
                        o.getEmployee() != null && o.getEmployee().getCode() != null && o.getCreatedByType() != null
                                && !OrderCreatorType.USER.equals(o.getCreatedByType()))
                        ? o.getEmployee().getCode()
                        : (o.getUser().getCode() != null ? o.getUser().getCode() : "");
                row.createCell(18).setCellValue(creatorCode);

                // Thời gian
                row.createCell(19).setCellValue(o.getCreatedAt() != null ? o.getCreatedAt().format(dtf) : "");
                row.createCell(20).setCellValue(o.getDeliveredAt() != null ? o.getDeliveredAt().format(dtf) : "N/A");
                row.createCell(21).setCellValue(o.getPaidAt() != null ? o.getPaidAt().format(dtf) : "N/A");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR);
        }
    }

    private <T> void updateManagerField(
            String fieldName,
            T oldValue,
            T newValue,
            OrderStatus currentStatus,
            OrderCreatorType creatorType,
            Consumer<T> setter) {

        if (OrderFieldUtils.isChanged(oldValue, newValue)) {
            if (!ManagerOrderEditRuleUtils.canManagerEditOrderField(fieldName, currentStatus, creatorType)) {
                throw new AppException(OrderErrorCode.ORDER_FIELD_UPDATE_DENIED, fieldName, currentStatus);
            }
            setter.accept(newValue);
        }
    }

    private void validateCreate(ManagerOrderCreateRequest request) {

        List<String> missing = new ArrayList<>();

        if (isBlank(request.getSenderName()))
            missing.add("Tên người gửi");
        if (isBlank(request.getSenderPhone()))
            missing.add("Số điện thoại người gửi");
        if (isBlank(request.getSenderDetail()))
            missing.add("Địa chỉ chi tiết người gửi");
        if (request.getSenderCityCode() == null)
            missing.add("Tỉnh/ thành phố người gửi");
        if (isBlank(request.getSenderCityName()))
            missing.add("Tên tỉnh/ thành phố người gửi");
        if (request.getSenderWardCode() == null)
            missing.add("Phường/ xã người gửi");
        if (isBlank(request.getSenderWardName()))
            missing.add("Tên phường/ xã người gửi");
        if (request.getSenderLatitude() == null)
            missing.add("Vĩ độ người gửi");
        if (request.getSenderLongitude() == null)
            missing.add("Kinh độ người gửi");

        if (isBlank(request.getRecipientName()))
            missing.add("Tên người nhận");
        if (isBlank(request.getRecipientPhone()))
            missing.add("Số điện thoại người nhận");
        if (isBlank(request.getRecipientDetail()))
            missing.add("Địa chỉ chi tiết người nhận");
        if (request.getRecipientCityCode() == null)
            missing.add("Tỉnh/ thành phố người nhận");
        if (isBlank(request.getRecipientCityName()))
            missing.add("Tên tỉnh/ thành phố người nhận");
        if (request.getRecipientWardCode() == null)
            missing.add("Phường/ xã người nhận");
        if (isBlank(request.getRecipientWardName()))
            missing.add("Tên phường/ xã người nhận");
        if (request.getRecipientLatitude() == null)
            missing.add("Vĩ độ người nhận");
        if (request.getRecipientLongitude() == null)
            missing.add("Kinh độ người nhận");

        if (request.getWeight() == null)
            missing.add("Khối lượng quy đổi");
        if (request.getHeight() == null)
            missing.add("Chiều cao");
        if (request.getOriginalWeight() == null)
            missing.add("Khối lượng thực tế");
        if (request.getLength() == null)
            missing.add("Chiều dài");
        if (request.getWidth() == null)
            missing.add("Chiều rộng");
        if (request.getServiceTypeId() == null)
            missing.add("Loại dịch vụ");
        if (request.getOrderValue() == null)
            missing.add("Giá trị đơn hàng");
        if (isBlank(request.getPayer()))
            missing.add("Người trả phí");

        if (!missing.isEmpty()) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, String.join(", ", missing));
        }

        try {
            OrderPayerType.valueOf(request.getPayer());
        } catch (Exception e) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_PAYER);
        }

        if (!request.getSenderPhone()
                .matches("\\d{10}"))
            throw new AppException(OrderErrorCode.ORDER_INVALID_SENDER_PHONE);
        if (!request.getRecipientPhone()
                .matches("\\d{10}"))
            throw new AppException(OrderErrorCode.ORDER_INVALID_RECIPIENT_PHONE);

        if (request.getSenderCityCode() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_SENDER_CITY_CODE);
        if (request.getSenderWardCode() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_SENDER_WARD_CODE);
        if (request.getSenderLatitude() < -90 || request.getSenderLatitude() > 90 || request.getSenderLongitude() < -180 || request.getSenderLongitude() > 180)
            throw new AppException(OrderErrorCode.ORDER_INVALID_SENDER_COORDINATES);

        if (request.getRecipientCityCode() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_RECIPIENT_CITY_CODE);
        if (request.getRecipientWardCode() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_RECIPIENT_WARD_CODE);

        if (request.getRecipientLatitude() < -90 || request.getRecipientLatitude() > 90 || request.getRecipientLongitude() < -180 || request.getRecipientLongitude() > 180)
            throw new AppException(OrderErrorCode.ORDER_INVALID_RECIPIENT_COORDINATES);

        if (request.getWeight().doubleValue() <= 0 || request.getOriginalWeight().doubleValue() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_WEIGHT);

        if (request.getLength().doubleValue() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_LENGTH);

        if (request.getWidth().doubleValue() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_WIDTH);

        if (request.getHeight().doubleValue() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_HEIGHT);

        if (request.getServiceTypeId() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_SERVICE_TYPE_ID);

        if (request.getOrderValue() != null && request.getOrderValue() < 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_VALUE);

        if (request.getNotes() != null && request.getNotes()
                .length() > 1000)
            throw new AppException(OrderErrorCode.ORDER_NOTE_TOO_LONG);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String generateTrackingNumber() {
        String prefix = "UTE";

        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));

        String random = java.util.UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();

        return prefix + date + random;
    }

    private String generateUniqueTrackingNumber(OrderStatus status) {
        if (status == OrderStatus.DRAFT)
            return null;

        String tracking;

        do {
            tracking = generateTrackingNumber();
        } while (repository.existsByTrackingNumber(tracking));

        return tracking;
    }

    public void setOrderAtOriginOffice(Integer userId, Integer orderId) {
        Order order = getOrderById(orderId);

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (order.getFromOffice() == null || !userOffice.getId()
                .equals(order.getFromOffice()
                        .getId())) {
            throw new AppException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        if (!OrderUtils.canManagerSetAtOriginOffice(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION,translateOrderStatus(order.getStatus()), translateOrderStatus(OrderStatus.AT_ORIGIN_OFFICE));
        }

        if (!order.getPickupType().equals(OrderPickupType.AT_OFFICE)) {
            throw new AppException(OrderErrorCode.ORDER_PICKUP_TYPE_INVALID);
        }

        order.setStatus(OrderStatus.AT_ORIGIN_OFFICE);
        repository.save(order);

        orderHistoryUserService.save(
                order,
                null,
                userOffice,
                null,
                OrderHistoryActionType.IMPORTED,
                null);

        if (order.getUser() != null) {
            notificationService.create(
                    "Đơn hàng của bạn đã được bàn giao cho đơn vị vận chuyển",
                    String.format(
                            "Đơn hàng có mã vận đơn #%s của bạn đã được mang đến và bàn giao cho đơn vị vận chuyển thành công tại bưu cục xuất phát. Nếu bạn không thực hiện hành động này, vui lòng liên hệ để được hỗ trợ.",
                            order.getTrackingNumber()),
                    "order",
                    order.getUser()
                            .getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
    }

    public void setOrderReturned(Integer userId, Integer orderId) {
        Order order = getOrderById(orderId);

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (order.getFromOffice() == null || !userOffice.getId()
                .equals(order.getFromOffice()
                        .getId())) {
            throw new AppException(OrderErrorCode.ORDER_ACCESS_DENIED);
        }

        if (!OrderUtils.canManagerSetReturned(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION,translateOrderStatus(order.getStatus()), translateOrderStatus(OrderStatus.AT_ORIGIN_OFFICE));
        }

        if (!order.getPickupType().equals(OrderPickupType.PICKUP_BY_COURIER)) {
            throw new AppException(OrderErrorCode.ORDER_PICKUP_TYPE_INVALID);
        }

        order.setStatus(OrderStatus.RETURNED);
        order.setReturnedAt(LocalDateTime.now());
        repository.save(order);

        orderHistoryUserService.save(
                order,
                null,
                userOffice,
                null,
                OrderHistoryActionType.RETURNED,
                null);

        if (order.getUser() != null) {
            notificationService.create(
                    "Hoàn hàng thành công",
                    String.format(
                            "Đơn hàng #%s đã được hoàn trả thành công đến người gửi.",
                            order.getTrackingNumber()),
                    "order",
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }
    }

    private void validateWeight(
            BigDecimal weight,
            BigDecimal originalWeight,
            BigDecimal height,
            BigDecimal length,
            BigDecimal width) {

        if (weight == null) {
            throw new IllegalArgumentException("Khối lượng không được để trống");
        }

        BigDecimal calWeight = feeService.calculateWeight(
                originalWeight, height, length, width
        );

        if (calWeight == null) {
            throw new IllegalArgumentException("Khối lượng quy đổi không hợp lệ.");
        }

        if (weight.compareTo(calWeight) != 0) {
            throw new IllegalArgumentException(
                    "Khối lượng không khớp với khối lượng đã tính trước đó"
            );
        }
    }

    private Order getOrderById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
    }
}