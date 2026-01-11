package com.logistics.service.manager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.request.manager.order.ManagerOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.manager.order.ManagerOrderDetailDto;
import com.logistics.dto.manager.order.ManagerOrderListDto;
import com.logistics.entity.Address;
import com.logistics.entity.Employee;
import com.logistics.entity.Office;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.OrderProduct;
import com.logistics.entity.ServiceType;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.OrderCreatorType;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.enums.OrderPayerType;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.OrderPickupType;
import com.logistics.enums.OrderStatus;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.OrderPrintMapper;
import com.logistics.repository.OfficeRepository;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderProductRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.common.FeePublicService;
import com.logistics.service.common.NotificationService;
import com.logistics.service.user.AddressUserService;
import com.logistics.service.user.OrderHistoryUserService;
import com.logistics.service.user.ProductUserService;
import com.logistics.service.user.PromotionUserService;
import com.logistics.service.user.ServiceTypeUserService;
import com.logistics.specification.OrderSpecification;
import com.logistics.utils.ManagerOrderEditRuleUtils;
import com.logistics.utils.OrderUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

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

    public ApiResponse<ListResponse<ManagerOrderListDto>> list(int userId, UserOrderSearchRequest request) {
        try {
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
            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;

            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
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
                default -> Sort.unsorted();
            };

            Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
            Page<Order> pageData = repository.findAll(spec, pageable);

            List<ManagerOrderListDto> list = pageData.getContent()
                    .stream()
                    .map(order -> OrderMapper.toManagerOrderListDto(order))
                    .toList();

            int total = (int) pageData.getTotalElements();

            Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

            ListResponse<ManagerOrderListDto> data = new ListResponse<>();
            data.setList(list);
            data.setPagination(pagination);

            return new ApiResponse<>(true, "Lấy danh sách đơn hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<List<Integer>> getAllOrderIds(int userId, UserOrderSearchRequest request) {
        try {
            String search = request.getSearch();
            String payer = request.getPayer();
            String status = request.getStatus();
            String pickupType = request.getPickupType();
            Integer serviceTypeId = request.getServiceTypeId();
            String paymentStatus = request.getPaymentStatus();
            String cod = request.getCod();
            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate())
                    : null;
            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
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

            List<Integer> orderIds = orders.stream()
                    .filter(order -> order.getTrackingNumber() != null)
                    .map(Order::getId)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách ID đơn hàng thành công", orderIds);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<ManagerOrderDetailDto> getOrderByTrackingNumber(int userId, String trackingNumber) {
        try {
            Order order = repository.findByTrackingNumber(trackingNumber)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            boolean hasAccess = (order.getFromOffice() != null
                    && userOffice.getId().equals(order.getFromOffice().getId()))
                    || (order.getToOffice() != null && userOffice.getId().equals(order.getToOffice().getId()));

            if (!hasAccess) {
                return new ApiResponse<>(false, "Bạn không có quyền xem đơn hàng này", null);
            }

            List<OrderHistory> orderHistories = orderHistoryRepository
                    .findByOrderIdOrderByActionTimeDesc(order.getId());

            List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());

            ManagerOrderDetailDto data = OrderMapper.toManagerOrderDetailDto(order, orderHistories, orderProducts);

            return new ApiResponse<>(true, "Lấy chi tiết đơn hàng theo mã đơn hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<List<OrderPrintDto>> getOrdersForPrint(Integer userId,
            List<Integer> orderIds) {
        try {
            List<Order> orders = repository.findByIdIn(orderIds);

            if (orders.isEmpty()) {
                return new ApiResponse<>(false, "Không tìm thấy đơn hàng nào để in", null);
            }

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            List<Order> printableOrders = orders.stream()
                    .filter(order -> OrderUtils.canManagerPrint(order.getStatus()))
                    .filter(order -> {
                        boolean fromMatch = order.getFromOffice() != null
                                && userOffice.getId().equals(order.getFromOffice().getId());
                        boolean toMatch = order.getToOffice() != null
                                && userOffice.getId().equals(order.getToOffice().getId());
                        return fromMatch || toMatch;
                    })
                    .toList();

            if (printableOrders.isEmpty()) {
                return new ApiResponse<>(false, "Không có đơn nào đủ điều kiện để in", null);
            }

            List<OrderPrintDto> printDtos = printableOrders.stream()
                    .map(order -> {
                        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
                        return OrderPrintMapper.toDto(order, orderProducts);
                    })
                    .toList();

            return new ApiResponse<>(true, "Lấy phiếu vận đơn thành công", printDtos);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy phiếu vận đơn: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> cancelOrder(Integer userId, Integer orderId) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (!OrderUtils.canManagerCancel(order.getStatus(), order.getCreatedByType())) {
            throw new RuntimeException("Đơn hàng đã vận chuyển, không thể hủy");
        }

        if (order.getFromOffice() == null || !userOffice.getId().equals(order.getFromOffice().getId())) {
            return new ApiResponse<>(false, "Bạn không có quyền hủy đơn hàng này", false);
        }

        order.setStatus(OrderStatus.CANCELLED);
        repository.save(order);

        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        productUserService.restoreStockFromOrder(orderProducts);

        if (order.getPromotion() != null) {
            promotionUserService.decreaseUsage(order.getPromotion().getId(), userId);
        }

        if (!order.getCreatedByType().equals(OrderCreatorType.USER) && order.getPayer().equals(OrderPayerType.SHOP)) {
            order.setRefundedAt(LocalDateTime.now());
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
                    order.getUser().getId(),
                    null,
                    "orders/list",
                    order.getTrackingNumber());
        }

        return new ApiResponse<>(true, "Hủy đơn hàng thành công", true);
    }

    public ApiResponse<Boolean> confirmOrder(Integer userId, Integer orderId) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (!OrderUtils.canManagerConfirm(order.getStatus(), order.getPickupType())) {
            throw new RuntimeException("Trạng thái đơn hàng hoặc hình thức lấy hàng không phù hợp");
        }

        if (order.getFromOffice() == null || !userOffice.getId().equals(order.getFromOffice().getId())) {
            return new ApiResponse<>(false, "Bạn không có quyền xác nhận đơn hàng này", false);
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
                    order.getUser().getId(),
                    null,
                    "orders/list",
                    order.getTrackingNumber());
        }

        return new ApiResponse<>(true, "Xác nhận đơn hàng thành công", true);
    }

    public ApiResponse<String> create(Integer userId, ManagerOrderCreateRequest request) {
        try {
            validateCreate(request);

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            Integer totalFee = 0;
            if (userOffice.getCityCode() != null) {
                totalFee = feeService.calculateTotalFeeManager(request.getWeight(), request.getServiceTypeId(),
                        userOffice.getCityCode(), request.getRecipientCityCode(),
                        request.getOrderValue(), 0);
            }

            Employee currentEmployee = userOffice.getManager();

            ServiceType serviceType = serviceTypeUserService.findById(request.getServiceTypeId())
                    .orElseThrow(() -> new RuntimeException("Dịch vụ vận chuyển không tồn tại"));

            Integer shippingFee = feeService.calculateShippingFee(
                    request.getWeight(), request.getServiceTypeId(),
                    userOffice.getCityCode(), request.getRecipientCityCode());

            Address address = Address.builder()
                    .wardCode(request.getRecipientWardCode())
                    .cityCode(request.getRecipientCityCode())
                    .detail(request.getRecipientDetail())
                    .name(request.getRecipientName())
                    .phoneNumber(request.getRecipientPhone())
                    .isDefault(false)
                    .build();
            Address recipientAddress = addressUserService.save(address);

            Order order = new Order();
            order.setStatus(OrderStatus.AT_ORIGIN_OFFICE);
            order.setTrackingNumber(generateUniqueTrackingNumber(order.getStatus()));
            order.setCreatedByType(OrderCreatorType.MANAGER);
            order.setSenderName(request.getSenderName());
            order.setSenderPhone(request.getSenderPhone());
            order.setSenderCityCode(request.getSenderCityCode());
            order.setSenderWardCode(request.getSenderWardCode());
            order.setSenderDetail(request.getSenderDetail());
            order.setRecipientName(request.getRecipientName());
            order.setRecipientPhone(request.getRecipientPhone());
            order.setRecipientAddress(recipientAddress);
            order.setPickupType(OrderPickupType.AT_OFFICE);
            order.setWeight(request.getWeight());
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
            if (OrderPayerType.valueOf(request.getPayer()).equals(OrderPayerType.SHOP)) {
                order.setPaymentStatus(OrderPaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
            }

            Order newOrder = repository.save(order);

            orderHistoryUserService.save(newOrder, null, userOffice,
                    null, OrderHistoryActionType.IMPORTED, null);

            return new ApiResponse<>(true, "Tạo đơn hàng thành công", newOrder.getTrackingNumber());
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> update(Integer userId, Integer orderId, ManagerOrderCreateRequest request) {
        try {
            validateCreate(request);

            Order order = repository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            BigDecimal oldWeight = order.getWeight();

            Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

            if (!ManagerOrderEditRuleUtils.canEditManagerOrder(order.getStatus())) {
                throw new RuntimeException("Đơn hàng đã hoàn thành, không thể chỉnh sửa");
            }

            if (!((order.getFromOffice() != null && userOffice.getId().equals(order.getFromOffice().getId()))
                    || (order.getToOffice() != null && userOffice.getId().equals(order.getToOffice().getId())))) {
                throw new RuntimeException("Bạn không có quyền sửa đơn hàng này");
            }

            OrderStatus currentStatus = order.getStatus();
            OrderCreatorType creatorType = order.getCreatedByType();

            Address recipient = order.getRecipientAddress();

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

            updateManagerField("senderWardCode",
                    order.getSenderWardCode(),
                    request.getSenderWardCode(),
                    currentStatus, creatorType,
                    order::setSenderWardCode);

            updateManagerField("senderDetailAddress",
                    order.getSenderDetail(),
                    request.getSenderDetail(),
                    currentStatus, creatorType,
                    order::setSenderDetail);

            // NGƯỜI NHẬN
            updateManagerField("recipientName",
                    recipient.getName(),
                    request.getRecipientName(),
                    currentStatus, creatorType,
                    recipient::setName);

            updateManagerField("recipientPhoneNumber",
                    recipient.getPhoneNumber(),
                    request.getRecipientPhone(),
                    currentStatus, creatorType,
                    recipient::setPhoneNumber);

            updateManagerField("recipientCityCode",
                    recipient.getCityCode(),
                    request.getRecipientCityCode(),
                    currentStatus, creatorType,
                    recipient::setCityCode);

            updateManagerField("recipientWardCode",
                    recipient.getWardCode(),
                    request.getRecipientWardCode(),
                    currentStatus, creatorType,
                    recipient::setWardCode);

            updateManagerField("recipientDetailAddress",
                    recipient.getDetail(),
                    request.getRecipientDetail(),
                    currentStatus, creatorType,
                    recipient::setDetail);

            if (request.getWeight() != null) {

                if (order.getUser() != null) {
                    updateManagerField(
                            "weight",
                            order.getWeight(),
                            request.getWeight(),
                            currentStatus,
                            creatorType,
                            order::setAdjustedWeight);
                } else {
                    updateManagerField(
                            "weight",
                            order.getWeight(),
                            request.getWeight(),
                            currentStatus,
                            creatorType,
                            order::setWeight);
                }

            }

            if (order.getUser() != null && request.getWeight() != null) {

                if (oldWeight == null || oldWeight.compareTo(request.getWeight()) != 0) {

                    int calcShippingFee = feeService.calculateShippingFee(request.getWeight(),
                            order.getServiceType().getId(),
                            order.getSenderCityCode(), order.getRecipientAddress().getCityCode());
                    order.setShippingFee(calcShippingFee);

                    int calcServiceFee = feeService.calculateTotalFee(
                            request.getWeight(),
                            order.getServiceType().getId(),
                            order.getSenderCityCode(),
                            order.getRecipientAddress().getCityCode(),
                            order.getOrderValue(),
                            order.getCod());

                    int discountAmount = 0;

                    if (order.getPromotion() != null) {
                        promotionUserService.decreaseUsage(order.getPromotion().getId(), userId);

                        order.setPromotion(null);
                    }

                    int calcTotalFee = calcServiceFee - discountAmount;
                    order.setDiscountAmount(discountAmount);
                    order.setTotalFee(calcTotalFee);

                    notificationService.create(
                            "Điều chỉnh khối lượng đơn hàng",
                            String.format(
                                    "Đơn hàng với mã vận đơn #%s phát hiện sai lệch khối lượng so với khai báo ban đầu. " +
                                            "Hệ thống đã tự động điều chỉnh từ %s kg thành %s kg, phí vận chuyển được cập nhật và khuyến mãi đã bị hủy.",
                                    order.getTrackingNumber(),
                                    oldWeight == null ? "0" : oldWeight.toPlainString(),
                                    request.getWeight().toPlainString()),
                            "order",
                            order.getUser().getId(),
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

            return new ApiResponse<>(true, "Cập nhật đơn hàng thành công", true);

        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    private <T> void updateManagerField(
            String field,
            T oldValue,
            T newValue,
            OrderStatus status,
            OrderCreatorType creatorType,
            java.util.function.Consumer<T> setter) {
        if (!Objects.equals(oldValue, newValue)) {

            if (!ManagerOrderEditRuleUtils.canManagerEditOrderField(field, status, creatorType)) {
                throw new RuntimeException(
                        "Trường '" + field + "' không thể chỉnh sửa ở trạng thái " + status);
            }

            setter.accept(newValue);
        }
    }

    private void validateCreate(ManagerOrderCreateRequest request) {

        List<String> missing = new ArrayList<>();

        if (isBlank(request.getSenderName()))
            missing.add("Tên người gửi");
        if (isBlank(request.getSenderDetail()))
            missing.add("Địa chỉ chi tiết người gửi");
        if (request.getSenderCityCode() == null)
            missing.add("Tỉnh/ thành phố người gửi");
        if (request.getSenderWardCode() == null)
            missing.add("Phường/ xã người gửi");
        if (isBlank(request.getSenderPhone()))
            missing.add("Số điện thoại người gửi");
        if (isBlank(request.getRecipientName()))
            missing.add("Tên người nhận");
        if (isBlank(request.getRecipientDetail()))
            missing.add("Địa chỉ chi tiết người nhận");
        if (request.getRecipientCityCode() == null)
            missing.add("Tỉnh/ thành phố người nhận");
        if (request.getRecipientWardCode() == null)
            missing.add("Phường/ xã người nhận");
        if (isBlank(request.getRecipientPhone()))
            missing.add("Số điện thoại người nhận");
        if (request.getWeight() == null)
            missing.add("Khối lượng");
        if (request.getServiceTypeId() == null)
            missing.add("Loại dịch vụ");
        if (request.getOrderValue() == null)
            missing.add("Giá trị đơn hàng");
        if (isBlank(request.getPayer()))
            missing.add("Người trả phí");

        if (!missing.isEmpty()) {
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missing));
        }

        try {
            OrderPayerType.valueOf(request.getPayer());
        } catch (Exception e) {
            throw new RuntimeException("Người trả phí không hợp lệ");
        }

        if (!request.getSenderPhone().matches("\\d{10}"))
            throw new RuntimeException("Số điện thoại người gửi phải gồm đúng 10 chữ số");

        if (!request.getRecipientPhone().matches("\\d{10}"))
            throw new RuntimeException("Số điện thoại người nhận phải gồm đúng 10 chữ số");

        if (request.getRecipientCityCode() <= 0)
            throw new RuntimeException("Mã Thành phố người gửi không hợp lệ");
        if (request.getRecipientWardCode() <= 0)
            throw new RuntimeException("Mã Phường/Xã người gửi không hợp lệ");

        if (request.getRecipientCityCode() <= 0)
            throw new RuntimeException("Mã Thành phố người nhận không hợp lệ");
        if (request.getRecipientWardCode() <= 0)
            throw new RuntimeException("Mã Phường/Xã người nhận không hợp lệ");

        if (request.getWeight().doubleValue() <= 0)
            throw new RuntimeException("Khối lượng phải lớn hơn 0");

        if (request.getServiceTypeId() <= 0)
            throw new RuntimeException("Mã dịch vụ không hợp lệ");

        if (request.getOrderValue() != null && request.getOrderValue() < 0)
            throw new RuntimeException("Giá trị đơn hàng phải lớn hơn hoặc bằng 0");

        if (request.getNotes() != null && request.getNotes().length() > 1000)
            throw new RuntimeException("Ghi chú tối đa 1000 ký tự");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String generateTrackingNumber() {
        String prefix = "UTE";

        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));

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

    public ApiResponse<Boolean> setOrderAtOriginOffice(Integer userId, Integer orderId) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);

        if (order.getFromOffice() == null || !userOffice.getId().equals(order.getFromOffice().getId())) {
            return new ApiResponse<>(false, "Bạn không có quyền hủy đơn hàng này", false);
        }

        if (!OrderUtils.canManagerSetAtOriginOffice(order.getStatus())) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ để chuyển");
        }

        if (!order.getPickupType().equals(OrderPickupType.AT_OFFICE)) {
            throw new RuntimeException("Hình thức lấy hàng không hợp lệ để chuyển");
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
                    order.getUser().getId(),
                    null,
                    "orders/tracking",
                    order.getTrackingNumber());
        }

        return new ApiResponse<>(true, "Bàn giao đơn hàng cho đơn vị vận chuyển thành công", true);
    }
}