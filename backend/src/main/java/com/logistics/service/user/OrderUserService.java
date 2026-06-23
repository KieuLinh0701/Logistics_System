package com.logistics.service.user;

import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.user.order.UserOrderDetailDto;
import com.logistics.dto.user.order.UserOrderListDto;
import com.logistics.dto.user.order.UserOrderStatusCountResponse;
import com.logistics.entity.*;
import com.logistics.enums.*;
import com.logistics.exception.AppException;
import com.logistics.exception.enums.*;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.OrderPrintMapper;
import com.logistics.repository.*;
import com.logistics.request.user.order.UserOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.OrderCreateSuccess;
import com.logistics.response.Pagination;
import com.logistics.service.common.FeePublicService;
import com.logistics.service.common.OfficePublicService;
import com.logistics.specification.OrderSpecification;
import com.logistics.utils.AddressUtils;
import com.logistics.utils.OrderFieldUtils;
import com.logistics.utils.OrderUtils;
import com.logistics.utils.UserOrderEditRuleUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.logistics.utils.OrderUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderUserService {

    private final OrderRepository repository;

    private final ProductRepository productRepository;

    private final OrderProductRepository orderProductRepository;

    private final OrderHistoryRepository orderHistoryRepository;

    private final PickupAttemptRepository pickupAttemptRepository;

    private final PromotionUserService promotionUserService;

    private final AddressUserService addressUserService;

    private final ServiceTypeUserService serviceTypeUserService;

    private final OfficePublicService officePublicService;

    private final FeePublicService feeService;

    private final UserUserService userUserService;

    private final ProductUserService productUserService;

    private final OrderHistoryUserService orderHistoryUserService;

    private final BankAccountRepository bankAccountRepository;

    public ListResponse<UserOrderListDto> list(int userId, UserOrderSearchRequest request) {
        Integer shopId = userUserService.getShopId(userId);

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

        Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                .and(OrderSpecification.userId(shopId))
                .and(OrderSpecification.search(search))
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

        List<UserOrderListDto> list = pageData.getContent()
                .stream()
                .map(order -> OrderMapper.toUserOrderListDto(order))
                .toList();

        int total = (int) pageData.getTotalElements();

        Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

        ListResponse<UserOrderListDto> data = new ListResponse<>();
        data.setList(list);
        data.setPagination(pagination);

        return data;
    }

    public List<UserOrderStatusCountResponse> getStatusCounts(Integer userId) {
        Integer shopId = userUserService.getShopId(userId);

        List<Object[]> raw = repository.countByStatusForUser(shopId);

        List<UserOrderStatusCountResponse> counts = raw.stream()
                .map(row -> new UserOrderStatusCountResponse(
                        ((OrderStatus) row[0]).name(),
                        (long) row[1]
                ))
                .collect(Collectors.toList());

        long total = counts.stream().mapToLong(UserOrderStatusCountResponse::getCount).sum();
        counts.addFirst(new UserOrderStatusCountResponse("ALL", total));

        return counts;

    }

    public List<Integer> getAllOrderIds(int userId, UserOrderSearchRequest request) {
        Integer shopId = userUserService.getShopId(userId);

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

        Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                .and(OrderSpecification.userId(shopId))
                .and(OrderSpecification.search(search))
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

    public OrderCreateSuccess create(Integer userId, UserOrderCreateRequest request) {
        validateCreate(request, false);

        Integer shopId = userUserService.getShopId(userId);

        User user = userUserService.getUser(shopId);
        if (user.getLocked()) {
            throw new AppException(AccountErrorCode.ACCOUNT_LOCKED_DUE_TO_OVERDUE);
        }

        if (!addressUserService.checkAddressBelongsToUser(request.getSenderAddressId(), shopId)) {
            throw new AppException(OrderErrorCode.ORDER_SENDER_ADDRESS_NOT_BELONG);
        }

        if (request.getFromOfficeId() != null) {
            if (!officePublicService.isSameCity(request.getSenderAddressId(), request.getFromOfficeId())) {
                throw new AppException(OrderErrorCode.ORDER_OFFICE_CITY_MISMATCH);
            }
        }

        if (!serviceTypeUserService.serviceTypeExists(request.getServiceTypeId())) {
            throw new AppException(ServiceTypeErrorCode.SERVICE_TYPE_NOT_FOUND);
        }

        if (request.getOrderProducts() != null && !request.getOrderProducts()
                .isEmpty()) {
            validateProductWithDB(shopId, request.getOrderProducts());
        }

        Address senderAddress = addressUserService.findByIdAndUserIdAndType(
                        request.getSenderAddressId(),
                        shopId,
                        AddressType.SENDER)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_RECIPIENT_ADDRESS_NOT_FOUND));

        BigDecimal weight = calculateWeight(
                request.getOrderProducts(),
                request.getOriginalWeight(),
                request.getHeight(),
                request.getLength(),
                request.getWidth());

        if (weight.compareTo(request.getWeight()) != 0 && request.getWeight() != null) {
            throw new AppException(OrderErrorCode.ORDER_PRODUCT_INFO_CHANGED);
        }

        Integer orderValue = calculateOrderValue(request.getOrderProducts(), request.getOrderValue());
        if (!orderValue.equals(request.getOrderValue()) && request.getOrderValue() != null) {
            throw new AppException(OrderErrorCode.ORDER_PRODUCT_INFO_CHANGED);
        }

        Integer serviceFee = feeService.calculateTotalFee(weight, request.getServiceTypeId(),
                senderAddress.getCityCode(), request.getRecipientCityCode(),
                orderValue, request.getCod());

        Promotion promotion = null;
        Integer discountAmount = 0;

        if (request.getPromotionId() != null && request.getPromotionId() > 0) {
            promotion = promotionUserService.findById(request.getPromotionId())
                    .orElseThrow(() -> new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND));

            if (!promotionUserService.canUsePromotion(
                    shopId,
                    request.getPromotionId(),
                    request.getServiceTypeId(),
                    serviceFee,
                    weight)) {
                throw new AppException(PromotionErrorCode.PROMOTION_NOT_ELIGIBLE);
            }

            discountAmount = promotionUserService.calculateDiscount(promotion, serviceFee);
        }

        if (request.getDiscountAmount() != null && !discountAmount.equals(request.getDiscountAmount())) {
            throw new AppException(PromotionErrorCode.PROMOTION_EXPIRED);
        }

        ServiceType serviceType = serviceTypeUserService.findById(request.getServiceTypeId())
                .orElseThrow(() -> new AppException(ServiceTypeErrorCode.SERVICE_TYPE_NOT_FOUND));

        Office fromOffice = null;
        if (request.getFromOfficeId() != null) {
            fromOffice = officePublicService.findById(request.getFromOfficeId())
                    .orElseThrow(() -> new AppException(OfficeErrorCode.OFFICE_NOT_FOUND));

            isOfficeLocatedIn(fromOffice, senderAddress.getCityCode());
        }

        boolean existBankAcc = existBankAccount(shopId);
        if (!existBankAcc) {
            throw new AppException(BankAccountErrorCode.BANK_ACCOUNT_REQUIRED);
        }

        Integer shippingFee = feeService.calculateShippingFee(
                weight, request.getServiceTypeId(),
                senderAddress.getCityCode(), request.getRecipientCityCode());

        if (request.getShippingFee() != null && !shippingFee.equals(request.getShippingFee())) {
            throw new AppException(OrderErrorCode.ORDER_SHIPPING_FEE_CHANGED);
        }

        Integer totalFee = serviceFee - discountAmount;

        Order order = new Order();
        order.setStatus(OrderStatus.valueOf(request.getStatus()));
        order.setTrackingNumber(generateUniqueTrackingNumber(order.getStatus()));
        order.setCreatedByType(OrderCreatorType.USER);
        order.setUser(user);
        order.setSenderName(senderAddress.getName());
        order.setSenderPhone(senderAddress.getPhoneNumber());
        order.setSenderCityCode(senderAddress.getCityCode());
        order.setSenderCityName(senderAddress.getCityName());
        order.setSenderWardName(senderAddress.getWardName());
        order.setSenderWardCode(senderAddress.getWardCode());
        order.setSenderDetail(senderAddress.getDetail());
        order.setSenderFullAddress(senderAddress.getFullAddress());
        order.setSenderLatitude(senderAddress.getLatitude());
        order.setSenderLongitude(senderAddress.getLongitude());
        order.setSenderAddress(senderAddress);
        order.setPickupType(OrderPickupType.valueOf(request.getPickupType()));
        order.setWeight(request.getWeight());
        order.setOriginalWeight(request.getOriginalWeight());
        order.setHeight(request.getHeight());
        order.setWidth(request.getWidth());
        order.setLength(request.getLength());
        order.setServiceType(serviceType);
        order.setPromotion(promotion);
        order.setDiscountAmount(discountAmount);
        order.setShippingFee(shippingFee);
        order.setCod(request.getCod());
        order.setOrderValue(orderValue);
        order.setTotalFee(totalFee);
        order.setPayer(OrderPayerType.valueOf(request.getPayer()));
        order.setPaymentStatus(OrderPaymentStatus.UNPAID);
        order.setNotes(request.getNotes());
        order.setFromOffice(fromOffice);
        order.setCodStatus(
                (request.getCod() != null && request.getCod() > 0)
                        ? OrderCodStatus.EXPECTED
                        : OrderCodStatus.NONE);

        Address recipientAddress = null;
        if (request.getRecipientAddressId() == null) {

            Optional<Address> existing = addressUserService
                    .findByPhoneNumberAndFullAddressAndUserIdAndType(
                            request.getRecipientPhone(),
                            AddressUtils.buildFullAddress(
                                    request.getRecipientDetail(),
                                    request.getRecipientWardName(),
                                    request.getRecipientCityName()
                            ),
                            shopId,
                            AddressType.RECIPIENT
                    );

            if (existing.isPresent()) {
                if (Boolean.TRUE.equals(request.getSaveRecipient())) {

                    Address addr = existing.get();

                    addr.setName(request.getRecipientName());
                    addr.setDetail(request.getRecipientDetail());
                    addr.setWardCode(request.getRecipientWardCode());
                    addr.setCityCode(request.getRecipientCityCode());
                    addr.setWardName(request.getRecipientWardName());
                    addr.setCityName(request.getRecipientCityName());
                    addr.setLatitude(request.getRecipientLatitude());
                    addr.setLongitude(request.getRecipientLongitude());

                    addr.setFullAddress(
                            AddressUtils.buildFullAddress(
                                    request.getRecipientDetail(),
                                    request.getRecipientWardName(),
                                    request.getRecipientCityName()
                            )
                    );

                    recipientAddress = addressUserService.save(addr);
                }
            } else if (Boolean.TRUE.equals(request.getSaveRecipient())) {

                recipientAddress = addressUserService.save(
                        Address.builder()
                                .name(request.getRecipientName())
                                .phoneNumber(request.getRecipientPhone())
                                .wardCode(request.getRecipientWardCode())
                                .cityCode(request.getRecipientCityCode())
                                .wardName(request.getRecipientWardName())
                                .cityName(request.getRecipientCityName())
                                .latitude(request.getRecipientLatitude())
                                .longitude(request.getRecipientLongitude())
                                .detail(request.getRecipientDetail())
                                .fullAddress(
                                        AddressUtils.buildFullAddress(
                                                request.getRecipientDetail(),
                                                request.getRecipientWardName(),
                                                request.getRecipientCityName()
                                        )
                                )
                                .isDefault(false)
                                .type(AddressType.RECIPIENT)
                                .user(user)
                                .build()
                );
            }
        } else {
            recipientAddress = addressUserService.findByIdAndUserIdAndType(
                            request.getRecipientAddressId(),
                            shopId,
                            AddressType.RECIPIENT)
                    .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_RECIPIENT_ADDRESS_NOT_FOUND));
        }

        order.setRecipientName(
                recipientAddress != null
                        ? recipientAddress.getName()
                        : request.getRecipientName()
        );

        order.setRecipientPhone(
                recipientAddress != null
                        ? recipientAddress.getPhoneNumber()
                        : request.getRecipientPhone()
        );

        order.setRecipientCityCode(
                recipientAddress != null
                        ? recipientAddress.getCityCode()
                        : request.getRecipientCityCode()
        );

        order.setRecipientCityName(
                recipientAddress != null
                        ? recipientAddress.getCityName()
                        : request.getRecipientCityName()
        );

        order.setRecipientWardCode(
                recipientAddress != null
                        ? recipientAddress.getWardCode()
                        : request.getRecipientWardCode()
        );

        order.setRecipientWardName(
                recipientAddress != null
                        ? recipientAddress.getWardName()
                        : request.getRecipientWardName()
        );

        order.setRecipientDetail(
                recipientAddress != null
                        ? recipientAddress.getDetail()
                        : request.getRecipientDetail()
        );

        order.setRecipientLatitude(
                recipientAddress != null
                        ? recipientAddress.getLatitude()
                        : request.getRecipientLatitude()
        );

        order.setRecipientLongitude(
                recipientAddress != null
                        ? recipientAddress.getLongitude()
                        : request.getRecipientLongitude()
        );

        order.setRecipientFullAddress(
                recipientAddress != null
                        ? recipientAddress.getFullAddress()
                        : AddressUtils.buildFullAddress(
                        request.getRecipientDetail(),
                        request.getRecipientWardName(),
                        request.getRecipientCityName()
                )
        );

        order.setRecipientAddress(recipientAddress);

        Order newOrder = repository.save(order);

        saveOrderProducts(newOrder, request.getOrderProducts());

        boolean isDraft = OrderStatus.DRAFT.name()
                .equals(request.getStatus());

        if (!isDraft) {
            orderHistoryUserService.save(newOrder, null, null,
                    null, OrderHistoryActionType.PENDING, null);

            if (promotion != null) {
                promotionUserService.increaseUsage(request.getPromotionId(), shopId);
            }
        }

        OrderCreateSuccess result = new OrderCreateSuccess();
        result.setOrderId(newOrder.getId());
        result.setTrackingNumber(newOrder.getTrackingNumber());

        return result;
    }

    public UserOrderDetailDto getOrderByTrackingNumber(int userId, String trackingNumber) {
        Integer shopId = userUserService.getShopId(userId);
        Order order = repository.findByTrackingNumberAndUserId(trackingNumber, shopId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        List<OrderHistory> orderHistories = orderHistoryRepository
                .findByOrderIdOrderByActionTimeDesc(order.getId());

        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        var pickupAttempts = pickupAttemptRepository.findByOrderIdOrderByAttemptedAtDesc(order.getId());

        return OrderMapper.toUserOrderDetailDto(order, orderHistories, orderProducts, pickupAttempts);
    }

    public UserOrderDetailDto getOrderById(int userId, int id) {
        Integer shopId = userUserService.getShopId(userId);

        Order order = getOrderByIdAndUserId(id, shopId);

        List<OrderHistory> orderHistories = orderHistoryRepository
                .findByOrderIdOrderByActionTimeDesc(order.getId());

        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        var pickupAttempts = pickupAttemptRepository.findByOrderIdOrderByAttemptedAtDesc(order.getId());

        return OrderMapper.toUserOrderDetailDto(order, orderHistories, orderProducts, pickupAttempts);
    }

    public String publicOrder(Integer userId, Integer orderId) {
        Integer shopId = userUserService.getShopId(userId);

        User user = userUserService.getUser(shopId);
        if (user.getLocked()) {
            throw new AppException(AccountErrorCode.ACCOUNT_LOCKED_DUE_TO_OVERDUE);
        }

        Order order = getOrderByIdAndUserId(orderId, shopId);

        if (!OrderUtils.canMoveToPending(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION, order.getStatus(), OrderStatus.PENDING);
        }

        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        for (OrderProduct op : orderProducts) {
            Product product = op.getProduct();
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new AppException(OrderErrorCode.ORDER_PRODUCT_INACTIVE, product.getName());
            }
            if (product.getStock() < op.getQuantity()) {
                throw new AppException(ProductErrorCode.PRODUCT_INSUFFICIENT_STOCK, product.getName(), product.getStock());
            }
        }

        Promotion promotion = order.getPromotion();
        if (promotion != null) {
            boolean canUse = promotionUserService.canUsePromotion(
                    shopId,
                    promotion.getId(),
                    order.getServiceType()
                            .getId(),
                    order.getShippingFee(),
                    order.getWeight());
            if (!canUse) {
                throw new AppException(PromotionErrorCode.PROMOTION_NOT_ELIGIBLE);
            }
        }

        for (OrderProduct op : orderProducts) {
            Product product = op.getProduct();
            int quantity = op.getQuantity();
            product.setStock(product.getStock() - quantity);
            product.setSoldQuantity(product.getSoldQuantity() + quantity);
            productRepository.save(product);
        }

        if (promotion != null) {
            promotionUserService.increaseUsage(promotion.getId(), shopId);
        }

        order.setStatus(OrderStatus.PENDING);

        String trackingNumber = generateUniqueTrackingNumber(order.getStatus());
        order.setTrackingNumber(trackingNumber);
        repository.save(order);

        orderHistoryUserService.save(order, null, null,
                null, OrderHistoryActionType.PENDING, null);

        return trackingNumber;
    }

    public void cancelOrder(Integer userId, Integer orderId) {
        Integer shopId = userUserService.getShopId(userId);
        Order order = getOrderByIdAndUserId(orderId, shopId);

        if (!OrderUtils.canUserCancel(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_CANNOT_CANCEL);
        }

        order.setStatus(OrderStatus.CANCELLED);
        repository.save(order);

        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        productUserService.restoreStockFromOrder(orderProducts);

        if (order.getPromotion() != null) {
            promotionUserService.decreaseUsage(order.getPromotion()
                    .getId(), shopId);
        }

        orderHistoryUserService.save(
                order,
                null,
                null,
                null,
                OrderHistoryActionType.CANCELLED,
                null);
    }

    @Transactional
    public void setOrderReadyForPickup(Integer userId, Integer orderId) {
        Integer shopId = userUserService.getShopId(userId);

        Order order = getOrderByIdAndUserId(orderId, shopId);

        if (!OrderUtils.canUserSetReady(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION, translateOrderStatus(order.getStatus()), translateOrderStatus(OrderStatus.READY_FOR_PICKUP));
        }

        if (!order.getPickupType()
                .equals(OrderPickupType.PICKUP_BY_COURIER)) {
            throw new AppException(OrderErrorCode.ORDER_PICKUP_TYPE_INVALID);
        }

        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        order.setReadyForPickupAt(LocalDateTime.now());
        order.setPickupNotificationStage(PickupNotificationStage.NONE);
        repository.save(order);

        orderHistoryUserService.save(
                order,
                null,
                null,
                null,
                OrderHistoryActionType.READY_FOR_PICKUP,
                null);
    }

    @Transactional
    public void setOrderTransitToOffice(Integer userId, Integer orderId) {
        Integer shopId = userUserService.getShopId(userId);

        Order order = getOrderByIdAndUserId(orderId, shopId);

        if (!OrderUtils.canUserSetTransitToOffice(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION, translateOrderStatus(order.getStatus()), translateOrderStatus(OrderStatus.TRANSIT_TO_OFFICE));
        }

        if (!order.getPickupType()
                .equals(OrderPickupType.AT_OFFICE)) {
            throw new AppException(OrderErrorCode.ORDER_PICKUP_TYPE_INVALID);
        }

        order.setStatus(OrderStatus.TRANSIT_TO_OFFICE);
        repository.save(order);

        orderHistoryUserService.save(
                order,
                null,
                null,
                null,
                OrderHistoryActionType.TRANSIT_TO_OFFICE,
                null);
    }

    @Transactional
    public void deleteOrder(Integer userId, Integer orderId) {
        Integer shopId = userUserService.getShopId(userId);
        // Lấy recipientaddress
        Order order = getOrderByIdAndUserId(orderId, shopId);

        if (!OrderUtils.canUserDelete(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_CANNOT_DELETE);
        }

        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        if (!orderProducts.isEmpty()) {
            orderProductRepository.deleteAll(orderProducts);
        }

        List<OrderHistory> histories = orderHistoryRepository.findByOrderId(order.getId());
        if (!histories.isEmpty()) {
            orderHistoryRepository.deleteAll(histories);
        }

        repository.delete(order);
    }

    @Transactional
    public List<OrderPrintDto> getOrdersForPrint(Integer userId, List<Integer> orderIds) {
        Integer shopId = userUserService.getShopId(userId);
        // Lấy danh sách đơn hàng theo userId và orderIds
        List<Order> orders = repository.findByUserIdAndIdIn(shopId, orderIds);

        if (orders.isEmpty()) {
            throw new AppException(OrderErrorCode.ORDER_NOT_FOUND_TO_PRINT);
        }

        // Lọc chỉ những đơn có thể in
        List<Order> printableOrders = orders.stream()
                .filter(order -> OrderUtils.canUserPrint(order.getStatus()))
                .toList();

        if (printableOrders.isEmpty()) {
            throw new AppException(OrderErrorCode.ORDER_NOT_FOUND_TO_PRINT);
        }

        // Chuyển sang DTO
        List<OrderPrintDto> printDtos = printableOrders.stream()
                .map(order -> {
                    List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
                    return OrderPrintMapper.toDto(order, orderProducts);
                })
                .toList();

        return printDtos;
    }

    @Transactional
    public void updateOrder(Integer userId, Integer orderId, UserOrderCreateRequest request) {
        // 1. Validate cơ bản
        validateCreate(request, true);

        Integer shopId = userUserService.getShopId(userId);

        // 2. Lấy recipientaddress
        Order order = getOrderByIdAndUserId(orderId, shopId);

        if (!UserOrderEditRuleUtils.canEditUserOrder(order.getStatus())) {
            throw new AppException(OrderErrorCode.ORDER_CANNOT_EDIT);
        }

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = OrderStatus.valueOf(request.getStatus());
        boolean movingDraftToPending = currentStatus == OrderStatus.DRAFT && newStatus == OrderStatus.PENDING;

        // 3. Chuyển trạng thái nếu từ DRAFT sang PENDING
        if (movingDraftToPending) {
            validateBeforePublish(shopId, request);
            order.setStatus(OrderStatus.PENDING);
            order.setTrackingNumber(generateUniqueTrackingNumber(order.getStatus()));
            orderHistoryUserService.save(order, null, null, null, OrderHistoryActionType.PENDING, null);

            List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
            for (OrderProduct op : orderProducts) {
                Product product = op.getProduct();
                int qty = op.getQuantity();
                product.setStock(product.getStock() - qty);
                product.setSoldQuantity(product.getSoldQuantity() + qty);
                productRepository.save(product);
            }
        } else if (currentStatus != newStatus) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION, currentStatus, newStatus);
        }

        // 4. Cập nhật các field theo rule
        if (request.getSenderAddressId() != null) {
            Address sender = order.getSenderAddress();

            Address reqSenderAddress = addressUserService.findByIdAndUserIdAndType(
                            request.getSenderAddressId(),
                            shopId,
                            AddressType.SENDER)
                    .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_SENDER_ADDRESS_NOT_FOUND));

            // Cập nhật toàn bộ object senderAddress
            updateFieldIfEditable("senderAddress", sender, reqSenderAddress,
                    currentStatus, order::setSenderAddress);

            // Cập nhật từng field để giữ consistency
            updateFieldIfEditable("senderName", sender.getName(), reqSenderAddress.getName(),
                    currentStatus, order::setSenderName);
            updateFieldIfEditable("senderPhoneNumber", sender.getPhoneNumber(),
                    reqSenderAddress.getPhoneNumber(),
                    currentStatus, order::setSenderPhone);
            updateFieldIfEditable("senderCityCode", sender.getCityCode(),
                    reqSenderAddress.getCityCode(),
                    currentStatus, order::setSenderCityCode);
            updateFieldIfEditable("senderCityName", sender.getCityName(),
                    reqSenderAddress.getCityName(),
                    currentStatus, order::setSenderCityName);
            updateFieldIfEditable("senderWardCode", sender.getWardCode(),
                    reqSenderAddress.getWardCode(),
                    currentStatus, order::setSenderWardCode);
            updateFieldIfEditable("senderWardName", sender.getWardName(),
                    reqSenderAddress.getWardName(),
                    currentStatus, order::setSenderWardName);
            updateFieldIfEditable("senderDetailAddress", sender.getDetail(),
                    reqSenderAddress.getDetail(),
                    currentStatus, order::setSenderDetail);
            updateFieldIfEditable("senderLatitude", sender.getLatitude(),
                    reqSenderAddress.getLatitude(),
                    currentStatus, order::setSenderLatitude);
            updateFieldIfEditable("senderLongitude", sender.getLongitude(),
                    reqSenderAddress.getLongitude(),
                    currentStatus, order::setSenderLongitude);

            order.setSenderFullAddress(AddressUtils.buildFullAddress(
                    order.getSenderDetail(),
                    order.getSenderWardName(),
                    order.getSenderCityName()
            ));
        }

        boolean hasRecipientAddressId = request.getRecipientAddressId() != null;

        Address newRecipientAddress = null;

        if (hasRecipientAddressId) {

            // Có recipientAddressId
            // -> chỉ gắn address, không update address và không update recipientaddress
            newRecipientAddress = addressUserService
                    .findByIdAndUserIdAndType(
                            request.getRecipientAddressId(),
                            shopId,
                            AddressType.RECIPIENT)
                    .orElseThrow(() ->
                            new AppException(OrderErrorCode.ORDER_RECIPIENT_ADDRESS_NOT_FOUND));

        } else {

            Optional<Address> existing = addressUserService
                    .findByPhoneNumberAndFullAddressAndUserIdAndType(
                            request.getRecipientPhone(),
                            AddressUtils.buildFullAddress(
                                    request.getRecipientDetail(),
                                    request.getRecipientWardName(),
                                    request.getRecipientCityName()
                            ),
                            shopId,
                            AddressType.RECIPIENT
                    );

            if (existing.isPresent()) {

                // Tìm thấy address cũ
                if (Boolean.TRUE.equals(request.getSaveRecipient())) {

                    newRecipientAddress = existing.get();

                    updateFieldIfEditable("recipientName",
                            newRecipientAddress.getName(),
                            request.getRecipientName(),
                            currentStatus,
                            newRecipientAddress::setName);

                    updateFieldIfEditable("recipientPhoneNumber",
                            newRecipientAddress.getPhoneNumber(),
                            request.getRecipientPhone(),
                            currentStatus,
                            newRecipientAddress::setPhoneNumber);

                    updateFieldIfEditable("recipientCityCode",
                            newRecipientAddress.getCityCode(),
                            request.getRecipientCityCode(),
                            currentStatus,
                            newRecipientAddress::setCityCode);

                    updateFieldIfEditable("recipientCityName",
                            newRecipientAddress.getCityName(),
                            request.getRecipientCityName(),
                            currentStatus,
                            newRecipientAddress::setCityName);

                    updateFieldIfEditable("recipientWardCode",
                            newRecipientAddress.getWardCode(),
                            request.getRecipientWardCode(),
                            currentStatus,
                            newRecipientAddress::setWardCode);

                    updateFieldIfEditable("recipientWardName",
                            newRecipientAddress.getWardName(),
                            request.getRecipientWardName(),
                            currentStatus,
                            newRecipientAddress::setWardName);

                    updateFieldIfEditable("recipientDetailAddress",
                            newRecipientAddress.getDetail(),
                            request.getRecipientDetail(),
                            currentStatus,
                            newRecipientAddress::setDetail);

                    updateFieldIfEditable("recipientLatitude",
                            newRecipientAddress.getLatitude(),
                            request.getRecipientLatitude(),
                            currentStatus,
                            newRecipientAddress::setLatitude);

                    updateFieldIfEditable("recipientLongitude",
                            newRecipientAddress.getLongitude(),
                            request.getRecipientLongitude(),
                            currentStatus,
                            newRecipientAddress::setLongitude);

                    newRecipientAddress.setFullAddress(
                            AddressUtils.buildFullAddress(
                                    newRecipientAddress.getDetail(),
                                    newRecipientAddress.getWardName(),
                                    newRecipientAddress.getCityName()
                            )
                    );

                    newRecipientAddress = addressUserService.save(newRecipientAddress);
                }

            } else {

                // Không tìm thấy -> tạo mới nếu muốn lưu
                if (Boolean.TRUE.equals(request.getSaveRecipient())) {

                    User user = userUserService.getUser(shopId);

                    newRecipientAddress = addressUserService.save(
                            Address.builder()
                                    .name(request.getRecipientName())
                                    .phoneNumber(request.getRecipientPhone())
                                    .cityCode(request.getRecipientCityCode())
                                    .cityName(request.getRecipientCityName())
                                    .wardCode(request.getRecipientWardCode())
                                    .wardName(request.getRecipientWardName())
                                    .detail(request.getRecipientDetail())
                                    .latitude(request.getRecipientLatitude())
                                    .longitude(request.getRecipientLongitude())
                                    .fullAddress(
                                            AddressUtils.buildFullAddress(
                                                    request.getRecipientDetail(),
                                                    request.getRecipientWardName(),
                                                    request.getRecipientCityName()
                                            )
                                    )
                                    .type(AddressType.RECIPIENT)
                                    .user(user)
                                    .build()
                    );
                }
            }
        }

        // Update recipientaddress
        updateFieldIfEditable("recipientName",
                order.getRecipientName(),
                newRecipientAddress != null
                        ? newRecipientAddress.getName()
                        : request.getRecipientName(),
                currentStatus,
                order::setRecipientName);

        updateFieldIfEditable("recipientPhoneNumber",
                order.getRecipientPhone(),
                newRecipientAddress != null
                        ? newRecipientAddress.getPhoneNumber()
                        : request.getRecipientPhone(),
                currentStatus,
                order::setRecipientPhone);

        updateFieldIfEditable("recipientCityCode",
                order.getRecipientCityCode(),
                newRecipientAddress != null
                        ? newRecipientAddress.getCityCode()
                        : request.getRecipientCityCode(),
                currentStatus,
                order::setRecipientCityCode);

        updateFieldIfEditable("recipientCityName",
                order.getRecipientCityName(),
                newRecipientAddress != null
                        ? newRecipientAddress.getCityName()
                        : request.getRecipientCityName(),
                currentStatus,
                order::setRecipientCityName);

        updateFieldIfEditable("recipientWardCode",
                order.getRecipientWardCode(),
                newRecipientAddress != null
                        ? newRecipientAddress.getWardCode()
                        : request.getRecipientWardCode(),
                currentStatus,
                order::setRecipientWardCode);

        updateFieldIfEditable("recipientWardName",
                order.getRecipientWardName(),
                newRecipientAddress != null
                        ? newRecipientAddress.getWardName()
                        : request.getRecipientWardName(),
                currentStatus,
                order::setRecipientWardName);

        updateFieldIfEditable("recipientDetailAddress",
                order.getRecipientDetail(),
                newRecipientAddress != null
                        ? newRecipientAddress.getDetail()
                        : request.getRecipientDetail(),
                currentStatus,
                order::setRecipientDetail);

        updateFieldIfEditable("recipientLatitude",
                order.getRecipientLatitude(),
                newRecipientAddress != null
                        ? newRecipientAddress.getLatitude()
                        : request.getRecipientLatitude(),
                currentStatus,
                order::setRecipientLatitude);

        updateFieldIfEditable("recipientLongitude",
                order.getRecipientLongitude(),
                newRecipientAddress != null
                        ? newRecipientAddress.getLongitude()
                        : request.getRecipientLongitude(),
                currentStatus,
                order::setRecipientLongitude);

        order.setRecipientFullAddress(
                AddressUtils.buildFullAddress(
                        order.getRecipientDetail(),
                        order.getRecipientWardName(),
                        order.getRecipientCityName()
                )
        );
        order.setRecipientAddress(newRecipientAddress);

        updateFieldIfEditable("pickupType", order.getPickupType()
                        .name(), request.getPickupType(),
                currentStatus,
                val -> order.setPickupType(OrderPickupType.valueOf(val)));

        Office fromOffice = null;
        if (request.getFromOfficeId() != null) {
            fromOffice = officePublicService.findById(request.getFromOfficeId())
                    .orElseThrow(() -> new AppException(OfficeErrorCode.OFFICE_NOT_FOUND));
            isOfficeLocatedIn(fromOffice, order.getSenderCityCode());
        }
        Office finalFromOffice = fromOffice;
        updateFieldIfEditable("fromOffice",
                order.getFromOffice() != null ? order.getFromOffice().getId() : null,
                request.getFromOfficeId(),
                currentStatus,
                val -> order.setFromOffice(val != null ? finalFromOffice : null));

        updateFieldIfEditable("payer", order.getPayer()
                        .name(), request.getPayer(), currentStatus,
                val -> order.setPayer(OrderPayerType.valueOf(val)));
        updateFieldIfEditable("notes", order.getNotes(), request.getNotes(), currentStatus,
                order::setNotes);

        // 5. Cập nhật products nếu có và được phép
        if (request.getOrderProducts() != null) {
            if (UserOrderEditRuleUtils.canEditUserOrderField("products", currentStatus)) {
                updateOrderProductsWithValidation(shopId, order, request.getOrderProducts(),
                        movingDraftToPending);
            }
        }

        // 6. Cập nhật weight và orderValue chỉ khi được phép
        BigDecimal calcWeight = calculateWeight(
                request.getOrderProducts(),
                request.getOriginalWeight(),
                request.getHeight(),
                request.getLength(),
                request.getWidth());
        int calcOrderValue = calculateOrderValue(request.getOrderProducts(), request.getOrderValue());

        updateFieldIfEditable("weight", order.getWeight(), calcWeight, currentStatus, order::setWeight);
        updateFieldIfEditable("originalWeight", order.getOriginalWeight(), request.getOriginalWeight()
                , currentStatus,
                order::setOriginalWeight);
        updateFieldIfEditable("height", order.getHeight(), request.getHeight(), currentStatus,
                order::setHeight);
        updateFieldIfEditable("length", order.getLength(), request.getLength(), currentStatus,
                order::setLength);
        updateFieldIfEditable("width", order.getWidth(), request.getWidth(), currentStatus,
                order::setWidth);
        updateFieldIfEditable("orderValue", order.getOrderValue(), calcOrderValue, currentStatus,
                order::setOrderValue);

        // 7. Áp dụng promotion nếu được phép
        if (UserOrderEditRuleUtils.canEditUserOrderField("promotion", currentStatus)) {
            Integer oldPromotionId = order.getPromotion() != null ? order.getPromotion()
                    .getId() : null;
            Integer newPromotionId = request.getPromotionId();

            boolean adjustPromotion = movingDraftToPending || order.getStatus() != OrderStatus.DRAFT;

            if (oldPromotionId != null && !Objects.equals(oldPromotionId, newPromotionId) && adjustPromotion) {
                promotionUserService.decreaseUsage(oldPromotionId, shopId);
            }

            if (newPromotionId != null) {
                Promotion promotion = promotionUserService.findById(newPromotionId)
                        .orElseThrow(() -> new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND));

                if (!promotionUserService.canUsePromotion(shopId, promotion.getId(), order.getServiceType()
                                .getId(),
                        order.getShippingFee(), order.getWeight())) {
                    throw new AppException(PromotionErrorCode.PROMOTION_NOT_ELIGIBLE);
                }

                order.setPromotion(promotion);
                order.setDiscountAmount(
                        promotionUserService.calculateDiscount(promotion, order.getShippingFee()));

                if (adjustPromotion) {
                    promotionUserService.increaseUsage(promotion.getId(), shopId);
                }
            } else {
                order.setPromotion(null);
                order.setDiscountAmount(0);
            }
        }

        // 8. Cập nhật shippingFee dựa trên weight và serviceType
        int calcShippingFee = feeService.calculateShippingFee(calcWeight, order.getServiceType().getId(),
                order.getSenderCityCode(), order.getRecipientCityCode());
        updateFieldIfEditable("shippingFee", order.getShippingFee(), calcShippingFee, currentStatus,
                order::setShippingFee);

        // 9. Cập nhật totalFee = shippingFee - discountAmount
        int calcServiceFee = feeService.calculateTotalFee(
                calcWeight,
                order.getServiceType().getId(),
                order.getSenderCityCode(),
                order.getRecipientCityCode(),
                calcOrderValue,
                order.getCod());

        int discountAmount = order.getPromotion() != null
                ? promotionUserService.calculateDiscount(order.getPromotion(), calcServiceFee)
                : 0;

        int calcTotalFee = calcServiceFee - discountAmount;
        order.setDiscountAmount(discountAmount);
        order.setTotalFee(calcTotalFee);

        repository.save(order);
    }

    public byte[] export(Integer userId, UserOrderSearchRequest request) {
        Integer shopId = userUserService.getShopId(userId);

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

        Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                .and(OrderSpecification.userId(shopId))
                .and(OrderSpecification.search(search))
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
            headerStyle.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x1C, (byte) 0x3D, (byte) 0x90}, null));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "Mã đơn",
                    "Trạng thái",
                    "Tên người nhận", "SĐT người nhận", "Địa chỉ người nhận",
                    "Khối lượng (Kg)", "Khối lượng điều chỉnh (Kg)",
                    "Hình thức lấy hàng", "Dịch vụ giao hàng",
                    "Thời gian tạo đơn", "Thời gian giao hàng", "Thời gian thanh toán",
                    "Người thanh toán", "Trạng thái thanh toán",
                    "Giá trị đơn", "COD (chưa phí)", "Phí dịch vụ",
                    "Người nhận trả",
                    "Người gửi trả",
                    "Còn nợ",
                    "COD thu về",
                    "Trạng thái COD"
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

                // Mã đơn
                row.createCell(0).setCellValue(o.getTrackingNumber() != null ? o.getTrackingNumber() : "Chưa có mã");

                // Trạng thái
                row.createCell(1).setCellValue(translateOrderStatus(o.getStatus()));

                // Người nhận
                row.createCell(2).setCellValue(o.getRecipientAddress() != null ? o.getRecipientAddress().getName() : "");
                row.createCell(3).setCellValue(o.getRecipientAddress() != null ? o.getRecipientAddress().getPhoneNumber() : "");
                row.createCell(4).setCellValue(o.getRecipientAddress() != null ? o.getRecipientAddress().getFullAddress() : "");

                // Khối lượng
                row.createCell(5).setCellValue(o.getWeight() != null ? o.getWeight().doubleValue() : 0.0);
                row.createCell(6).setCellValue(o.getAdjustedWeight() != null ? o.getAdjustedWeight().doubleValue() : 0.0);

                // Thông tin giao hàng
                row.createCell(7).setCellValue(translateOrderPickupType(o.getPickupType()));
                row.createCell(8).setCellValue(o.getServiceType().getName() != null ? o.getServiceType().getName() : "");

                // Thời gian
                row.createCell(9).setCellValue(o.getCreatedAt() != null ? o.getCreatedAt().format(dtf) : "");
                row.createCell(10).setCellValue(o.getDeliveredAt() != null ? o.getDeliveredAt().format(dtf) : "");
                row.createCell(11).setCellValue(o.getPaidAt() != null ? o.getPaidAt().format(dtf) : "");

                // Thanh toán
                row.createCell(12).setCellValue(translateOrderPayerType(o.getPayer()));
                row.createCell(13).setCellValue(translateOrderPaymentStatus(o.getPaymentStatus()));

                // Tổng quan tiền
                double orderValue = o.getOrderValue() != null ? o.getOrderValue() : 0;
                double codVal = o.getCod() != null ? o.getCod() : 0;
                double totalFee = o.getTotalFee() != null ? o.getTotalFee() : 0;

                row.createCell(14).setCellValue(orderValue);
                row.createCell(15).setCellValue(codVal);
                row.createCell(16).setCellValue(totalFee);

                // Người nhận trả
                double recipientPay = (o.getPayer() == OrderPayerType.CUSTOMER)
                        ? codVal + totalFee
                        : codVal;
                row.createCell(17).setCellValue(recipientPay);

                // Người gửi trả
                double senderPay = (o.getPayer() == OrderPayerType.SHOP) ? totalFee : 0;
                row.createCell(18).setCellValue(senderPay);

                // Còn nợ
                double debt = 0;
                if (o.getPayer() == OrderPayerType.SHOP) {
                    double diff = codVal - totalFee;
                    debt = diff > 0 ? 0 : Math.abs(diff);
                }
                row.createCell(19).setCellValue(debt);

                // COD thu về
                double codCollected = !(o.getPayer() == OrderPayerType.SHOP) ? codVal : Math.max(0, codVal - totalFee);
                row.createCell(20).setCellValue(codCollected);

                // Trạng thái COD
                row.createCell(21).setCellValue(translateOrderCodStatus(o.getCodStatus()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new AppException(CommonErrorCode.EXPORT_EXCEL_ERROR, e);
        }
    }

    private void updateOrderProductsWithValidation(Integer userId, Order order,
                                                   List<UserOrderCreateRequest.OrderProduct> items, boolean movingDraftToPending) {

        Integer shopId = userUserService.getShopId(userId);

        List<OrderProduct> existingProducts = orderProductRepository.findByOrderId(order.getId());
        boolean adjustStock = movingDraftToPending || order.getStatus() != OrderStatus.DRAFT;

        if (items.isEmpty()) {
            for (OrderProduct oldOp : existingProducts) {
                Product product = oldOp.getProduct();
                if (adjustStock) {
                    product.setStock(product.getStock() + oldOp.getQuantity());
                    product.setSoldQuantity(product.getSoldQuantity() - oldOp.getQuantity());
                    productRepository.save(product);
                }
                orderProductRepository.delete(oldOp);
            }
            return;
        }

        Map<Integer, OrderProduct> existingMap = new HashMap<>();
        for (OrderProduct op : existingProducts)
            existingMap.put(op.getProduct()
                    .getId(), op);

        for (var item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));

            if (!product.getUser()
                    .getId()
                    .equals(shopId))
                throw new AppException(ProductErrorCode.PRODUCT_NOT_OWNED, product.getName());

            if (product.getStatus() != ProductStatus.ACTIVE)
                throw new AppException(ProductErrorCode.PRODUCT_INACTIVE, product.getName());

            OrderProduct existing = existingMap.get(product.getId());
            int oldQty = existing != null ? existing.getQuantity() : 0;
            int stockAvailable = product.getStock() + (adjustStock && existing != null ? oldQty : 0);

            if (item.getQuantity() > stockAvailable)
                throw new AppException(ProductErrorCode.PRODUCT_INSUFFICIENT_STOCK, product.getName(), stockAvailable);

            if (adjustStock) {
                int delta = item.getQuantity() - oldQty;
                product.setStock(product.getStock() - delta);
                product.setSoldQuantity(product.getSoldQuantity() + delta);
                productRepository.save(product);
            }

            OrderProduct op = existing != null ? existing : new OrderProduct();
            op.setOrder(order);
            op.setProduct(product);
            op.setQuantity(item.getQuantity());
            op.setPrice(product.getPrice());
            orderProductRepository.save(op);
        }

        for (OrderProduct oldOp : existingProducts) {
            if (items.stream()
                    .noneMatch(i -> i.getProductId()
                            .equals(oldOp.getProduct()
                                    .getId()))) {
                Product product = oldOp.getProduct();
                if (adjustStock) {
                    product.setStock(product.getStock() + oldOp.getQuantity());
                    product.setSoldQuantity(product.getSoldQuantity() - oldOp.getQuantity());
                    productRepository.save(product);
                }
                orderProductRepository.delete(oldOp);
            }
        }
    }

    private <T> void updateFieldIfEditable(
            String fieldName,
            T oldValue,
            T newValue
            , OrderStatus currentStatus,
            Consumer<T> setter) {

        if (OrderFieldUtils.isChanged(oldValue, newValue)) {
            var rule = UserOrderEditRuleUtils.USER_ORDER_FIELD_EDIT_RULES.get(fieldName);
            if (rule != null) {
                // Nếu nonEditableStatuses chứa currentStatus → không được sửa
                if (rule.getNonEditableStatuses() != null && !rule.getNonEditableStatuses().isEmpty()) {
                    if (rule.getNonEditableStatuses().contains(currentStatus)) {
                        throw new AppException(OrderErrorCode.ORDER_FIELD_UPDATE_DENIED, fieldName, currentStatus);
                    }
                }

                // Nếu editableStatuses != null → chỉ cho phép những trạng thái đó
                if (rule.getEditableStatuses() != null && !rule.getEditableStatuses().isEmpty()) {
                    if (!rule.getEditableStatuses().contains(currentStatus)) {
                        throw new AppException(OrderErrorCode.ORDER_FIELD_UPDATE_DENIED, fieldName, currentStatus);
                    }
                }
            }

            setter.accept(newValue);
        }
    }

    private void validateBeforePublish(Integer userId, UserOrderCreateRequest request) {

        User user = userUserService.getUser(userId);
        if (user.getLocked()) {
            throw new AppException(AccountErrorCode.ACCOUNT_LOCKED_DUE_TO_OVERDUE);
        }

        // Kiểm tra các field bắt buộc
        validateCreate(request, true);

        // Kiểm tra sản phẩm
        if (request.getOrderProducts() != null && !request.getOrderProducts()
                .isEmpty()) {
            validateProductWithDB(userId, request.getOrderProducts());
        }

        // Kiểm tra promotion nếu có
        if (request.getPromotionId() != null) {
            Promotion promotion = promotionUserService.findById(request.getPromotionId())
                    .orElseThrow(() -> new AppException(PromotionErrorCode.PROMOTION_NOT_FOUND));

            if (!promotionUserService.canUsePromotion(
                    userId,
                    promotion.getId(),
                    request.getServiceTypeId(),
                    request.getShippingFee(),
                    calculateWeight(
                            request.getOrderProducts(),
                            request.getOriginalWeight(),
                            request.getHeight(),
                            request.getLength(),
                            request.getWidth()))) {
                throw new AppException(PromotionErrorCode.PROMOTION_NOT_ELIGIBLE);
            }
        }
    }

    private boolean existBankAccount(Integer userId) {
        return bankAccountRepository.existsByUserId(userId);
    }

    private void validateCreate(UserOrderCreateRequest request, boolean isEdit) {

        List<String> missing = new ArrayList<>();

        if (isBlank(request.getStatus()))
            missing.add("Trạng thái đơn hàng");
        if (request.getSenderAddressId() == null && !isEdit)
            missing.add("Địa chỉ người gửi");
        if (isBlank(request.getPickupType()))
            missing.add("Hình thức giao hàng");
        if (request.getWeight() == null)
            missing.add("Khối lượng quy đổi");
        if (request.getOriginalWeight() == null)
            missing.add("Khối lượng thực tế");
        if (request.getHeight() == null)
            missing.add("Chiều cao");
        if (request.getLength() == null)
            missing.add("Chiều dài");
        if (request.getWidth() == null)
            missing.add("Chiều rộng");
        if (request.getServiceTypeId() == null)
            missing.add("Loại dịch vụ");
        if (request.getCod() == null)
            missing.add("Phí thu hộ");

        if ((request.getOrderProducts() == null || request.getOrderProducts()
                .isEmpty())
                && request.getOrderValue() == null)
            missing.add("Giá trị đơn hàng");

        if (isBlank(request.getPayer()))
            missing.add("Người trả phí");

        if (!missing.isEmpty()) {
            throw new AppException(CommonErrorCode.MISSING_REQUIRED_FIELDS, missing);
        }

        boolean isNewRecipient = request.getRecipientAddressId() == null;

        boolean missingRecipientInfo =
                isBlank(request.getRecipientName())
                        || isBlank(request.getRecipientPhone())
                        || request.getRecipientCityCode() == null
                        || request.getRecipientWardCode() == null
                        || isBlank(request.getRecipientDetail())
                        || isBlank(request.getRecipientCityName())
                        || isBlank(request.getRecipientWardName())
                        || request.getRecipientLongitude() == null
                        || request.getRecipientLatitude() == null;

        if (isNewRecipient && missingRecipientInfo) {
            missing.add("Địa chỉ người nhận");
        }

        try {
            OrderStatus.valueOf(request.getStatus());
        } catch (Exception e) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_STATUS);
        }

        try {
            OrderPickupType.valueOf(request.getPickupType());
        } catch (Exception e) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_PICKUP_TYPE);
        }

        try {
            OrderPayerType.valueOf(request.getPayer());
        } catch (Exception e) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_PAYER);
        }

        if (request.getSenderAddressId() != null && request.getSenderAddressId() <= 0)
            throw new AppException(OrderErrorCode.ORDER_SENDER_ADDRESS_INVALID);

        if (!isNewRecipient && request.getRecipientAddressId() != null && request.getRecipientAddressId() <= 0)
            throw new AppException(OrderErrorCode.ORDER_RECIPIENT_ADDRESS_INVALID);

        if (!request.getRecipientPhone()
                .matches("\\d{10}"))
            throw new AppException(OrderErrorCode.ORDER_INVALID_RECIPIENT_PHONE);

        if (isNewRecipient && request.getRecipientCityCode() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_RECIPIENT_CITY_CODE);
        if (isNewRecipient && request.getRecipientWardCode() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_RECIPIENT_WARD_CODE);

        if (isNewRecipient) {
            Double lat = request.getRecipientLatitude();
            Double lng = request.getRecipientLongitude();

            if (lat == null || lng == null
                    || lat < -90 || lat > 90
                    || lng < -180 || lng > 180) {
                throw new AppException(OrderErrorCode.ORDER_INVALID_RECIPIENT_COORDINATES);
            }
        }

        if (request.getWeight()
                .doubleValue() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_WEIGHT);

        if (request.getOriginalWeight()
                .doubleValue() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_WEIGHT);

        if (request.getHeight()
                .doubleValue() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_HEIGHT);

        if (request.getLength()
                .doubleValue() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_LENGTH);

        if (request.getWidth()
                .doubleValue() <= 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_WIDTH);

        if (request.getServiceTypeId() <= 0)
            throw new AppException(ServiceTypeErrorCode.SERVICE_TYPE_INVALID);

        if (request.getCod() < 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_COD_VALUE);

        if (request.getOrderValue() != null && request.getOrderValue() < 0)
            throw new AppException(OrderErrorCode.ORDER_INVALID_ORDER_VALUE);

        if (request.getNotes() != null && request.getNotes()
                .length() > 1000)
            throw new AppException(OrderErrorCode.ORDER_NOTE_TOO_LONG);

        if (request.getPromotionId() != null && request.getPromotionId() <= 0)
            throw new AppException(PromotionErrorCode.PROMOTION_INVALID);

        if (OrderPickupType.AT_OFFICE.name()
                .equals(request.getPickupType())) {
            if (request.getFromOfficeId() == null)
                throw new AppException(OrderErrorCode.ORDER_FROM_OFFICE_REQUIRED);
        }

        if (request.getOrderProducts() != null) {
            int index = 1;
            for (var op : request.getOrderProducts()) {
                if (op.getProductId() == null || op.getProductId() <= 0)
                    throw new AppException(OrderErrorCode.ORDER_INVALID_PRODUCT);
                if (op.getQuantity() == null || op.getQuantity() <= 0)
                    throw new AppException(OrderErrorCode.ORDER_INVALID_PRODUCT_QUANTITY);
                index++;
            }
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private void validateProductWithDB(int userId, List<UserOrderCreateRequest.OrderProduct> items) {
        for (var item : items) {

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));

            if (!product.getUser()
                    .getId()
                    .equals(userId)) {
                throw new AppException(ProductErrorCode.PRODUCT_NOT_OWNED);
            }

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new AppException(ProductErrorCode.PRODUCT_INACTIVE, product.getName());
            }

            if (product.getStock() <= 0) {
                throw new AppException(ProductErrorCode.PRODUCT_OUT_OF_STOCK, product.getName());
            }

            if (item.getQuantity() > product.getStock()) {
                throw new AppException(ProductErrorCode.PRODUCT_INSUFFICIENT_STOCK, product.getName(), product.getStock());
            }
        }
    }

    private String generateTrackingNumber() {
        String prefix = "UTE";

        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyMMdd"));

        String random = UUID.randomUUID()
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

    public BigDecimal calculateWeight(
            List<UserOrderCreateRequest.OrderProduct> items,
            BigDecimal originalWeight,
            BigDecimal height,
            BigDecimal length,
            BigDecimal width) {
        if (items == null || items.isEmpty()) {
            return feeService.calculateWeight(originalWeight, height, length, width);
        }

        BigDecimal totalOriginalWeight = BigDecimal.ZERO;
        for (var item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow();
            BigDecimal itemWeight = product.getWeight()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalOriginalWeight = totalOriginalWeight.add(itemWeight);
        }

        return feeService.calculateWeight(totalOriginalWeight, height, length, width);
    }

    public int calculateOrderValue(List<UserOrderCreateRequest.OrderProduct> items, Integer orderValue) {
        if (items == null || items.isEmpty()) {
            return orderValue != null ? orderValue : 0;
        }

        int total = 0;
        for (var item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow();
            total += product.getPrice() * item.getQuantity();
        }

        return total;
    }

    @Transactional
    public void saveOrderProducts(Order order, List<UserOrderCreateRequest.OrderProduct> items) {
        if (items == null || items.isEmpty())
            return;

        boolean isDraft = order.getStatus() == OrderStatus.DRAFT;

        for (var item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));

            if (!isDraft) {
                int newStock = product.getStock() - item.getQuantity();
                if (newStock < 0)
                    throw new AppException(ProductErrorCode.PRODUCT_INSUFFICIENT_STOCK, product.getName(), product.getStock());
                product.setStock(newStock);
                product.setSoldQuantity(product.getSoldQuantity() + item.getQuantity());
                productRepository.save(product);
            }

            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setOrder(order);
            orderProduct.setProduct(product);
            orderProduct.setQuantity(item.getQuantity());
            orderProduct.setPrice(product.getPrice());
            orderProductRepository.save(orderProduct);
        }
    }

    private Order getOrderByIdAndUserId(Integer id, Integer shopId) {
        return repository.findByIdAndUserId(id, shopId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * Kiểm tra bưu cục có thuộc quyền quản lý/phục vụ khu vực đó không
     */
    private void isOfficeLocatedIn(Office office, int cityCode) {
        if (office == null) {
            throw new AppException(OfficeErrorCode.OFFICE_NOT_FOUND);
        }
        if (office.getCityCode() != cityCode) {
            throw new AppException(OfficeErrorCode.OFFICE_LOCATION_MISMATCH);
        }
    }
}
