package com.logistics.service.user;

import com.logistics.dto.manager.employee.ManagerEmployeePerformanceDto;
import com.logistics.dto.user.order.UserOrderStatusCountResponse;
import com.logistics.enums.AddressType;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.logistics.request.SearchRequest;
import com.logistics.utils.AddressUtils;
import com.logistics.utils.OrderFieldUtils;
import com.logistics.utils.PaymentSubmissionBatchUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

import com.logistics.request.user.order.UserOrderCreateRequest;
import com.logistics.request.user.order.UserOrderSearchRequest;
import com.logistics.dto.OrderPrintDto;
import com.logistics.dto.user.order.UserOrderDetailDto;
import com.logistics.dto.user.order.UserOrderListDto;
import com.logistics.entity.Address;
import com.logistics.entity.Office;
import com.logistics.entity.Order;
import com.logistics.entity.OrderHistory;
import com.logistics.entity.OrderProduct;
import com.logistics.entity.Product;
import com.logistics.entity.Promotion;
import com.logistics.entity.ServiceType;
import com.logistics.entity.User;
import com.logistics.enums.OrderCodStatus;
import com.logistics.enums.OrderCreatorType;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.enums.OrderPayerType;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.OrderPickupType;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ProductStatus;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.OrderPrintMapper;
import com.logistics.repository.BankAccountRepository;
import com.logistics.repository.OrderHistoryRepository;
import com.logistics.repository.OrderProductRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.ProductRepository;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.OrderCreateSuccess;
import com.logistics.response.Pagination;
import com.logistics.service.common.FeePublicService;
import com.logistics.service.common.OfficePublicService;
import com.logistics.specification.OrderSpecification;
import com.logistics.utils.OrderUtils;
import com.logistics.utils.UserOrderEditRuleUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import static com.logistics.utils.OrderUtils.translateOrderCodStatus;
import static com.logistics.utils.OrderUtils.translateOrderPayerType;
import static com.logistics.utils.OrderUtils.translateOrderPaymentStatus;
import static com.logistics.utils.OrderUtils.translateOrderPickupType;
import static com.logistics.utils.OrderUtils.translateOrderStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderUserService {

    private final OrderRepository repository;

    private final ProductRepository productRepository;

    private final OrderProductRepository orderProductRepository;

    private final OrderHistoryRepository orderHistoryRepository;

    private final PromotionUserService promotionUserService;

    private final AddressUserService addressUserService;

    private final ServiceTypeUserService serviceTypeUserService;

    private final OfficePublicService officePublicService;

    private final FeePublicService feeService;

    private final UserUserService userUserService;

    private final ProductUserService productUserService;

    private final OrderHistoryUserService orderHistoryUserService;

    private final BankAccountRepository bankAccountRepository;

    public ApiResponse<ListResponse<UserOrderListDto>> list(int userId, UserOrderSearchRequest request) {
        try {
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

            return new ApiResponse<>(true, "Lấy danh sách đơn hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<List<UserOrderStatusCountResponse>> getStatusCounts(Integer userId) {
        Integer shopId = userUserService.getShopId(userId);

        List<Object[]> raw = repository.countByStatusForUser(shopId);

        List<UserOrderStatusCountResponse> counts = raw.stream()
                .map(row -> new UserOrderStatusCountResponse(
                        ((OrderStatus) row[0]).name(),
                        (long) row[1]
                ))
                .collect(Collectors.toList());

        long total = counts.stream().mapToLong(UserOrderStatusCountResponse::getCount).sum();
        counts.add(0, new UserOrderStatusCountResponse("ALL", total));

        return new ApiResponse<>(true, "Lấy số lượng theo trạng thái thành công", counts);

    }

    public ApiResponse<List<Integer>> getAllOrderIds(int userId, UserOrderSearchRequest request) {
        try {
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

            List<Integer> orderIds = orders.stream()
                    .filter(order -> order.getTrackingNumber() != null)
                    .map(Order::getId)
                    .toList();

            return new ApiResponse<>(true, "Lấy toàn bộ ID đơn hàng thành công", orderIds);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<OrderCreateSuccess> create(Integer userId, UserOrderCreateRequest request) {
        try {
            validateCreate(request, false);

            Integer shopId = userUserService.getShopId(userId);

            User user = userUserService.getUser(shopId);
            if (user.getLocked()) {
                return new ApiResponse<>(false,
                        "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới.",
                        null);
            }

            if (!addressUserService.checkAddressBelongsToUser(request.getSenderAddressId(), shopId)) {
                return new ApiResponse<>(false, "Địa chỉ người gửi không thuộc người dùng", null);
            }

            if (request.getFromOfficeId() != null) {
                if (!officePublicService.isSameCity(request.getSenderAddressId(), request.getFromOfficeId())) {
                    return new ApiResponse<>(false, "Địa chỉ gửi và bưu cục phải thuộc cùng thành phố", null);
                }
            }

            if (!serviceTypeUserService.serviceTypeExists(request.getServiceTypeId())) {
                return new ApiResponse<>(false, "Dịch vụ vận chuyển không tồn tại", null);
            }

            if (request.getOrderProducts() != null && !request.getOrderProducts()
                    .isEmpty()) {
                validateProductWithDB(shopId, request.getOrderProducts());
            }

            Address senderAddress = addressUserService.findByIdAndUserIdAndType(
                            request.getSenderAddressId(),
                            shopId,
                            AddressType.SENDER)
                    .orElseThrow(() -> new RuntimeException("Địa chỉ người gửi không tồn tại"));

            BigDecimal weight = calculateWeight(
                    request.getOrderProducts(),
                    request.getOriginalWeight(),
                    request.getHeight(),
                    request.getLength(),
                    request.getWidth());

            if (weight.compareTo(request.getWeight()) != 0 && request.getWeight() != null) {
                return new ApiResponse<>(
                        false,
                        "Thông tin của sản phẩm đã thay đổi. Vui lòng kiểm tra lại các sản phẩm đã chọn trước khi tạo đơn.",
                        null);
            }

            Integer orderValue = calculateOrderValue(request.getOrderProducts(), request.getOrderValue());
            if (!orderValue.equals(request.getOrderValue()) && request.getOrderValue() != null) {
                return new ApiResponse<>(
                        false,
                        "Thông tin của sản phẩm đã thay đổi. Vui lòng kiểm tra lại các sản phẩm đã chọn trước khi tạo đơn.",
                        null);
            }

            Integer serviceFee = feeService.calculateTotalFee(weight, request.getServiceTypeId(),
                    senderAddress.getCityCode(), request.getRecipientCityCode(),
                    orderValue, request.getCod());

            Promotion promotion = null;
            Integer discountAmount = 0;

            if (request.getPromotionId() != null && request.getPromotionId() > 0) {
                promotion = promotionUserService.findById(request.getPromotionId())
                        .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));

                if (!promotionUserService.canUsePromotion(
                        shopId,
                        request.getPromotionId(),
                        request.getServiceTypeId(),
                        serviceFee,
                        weight)) {
                    return new ApiResponse<>(false, "Bạn không đủ điều kiện để dùng mã giảm giá", null);
                }

                discountAmount = promotionUserService.calculateDiscount(promotion, serviceFee);
            }

            if (request.getDiscountAmount() != null && !discountAmount.equals(request.getDiscountAmount())) {
                return new ApiResponse<>(
                        false,
                        "Rất tiếc! Khuyến mãi bạn chọn có thể đã thay đổi, hết hạn hoặc hết lượt sử dụng. Vui lòng kiểm tra lại trước khi đặt hàng.",
                        null);
            }

            ServiceType serviceType = serviceTypeUserService.findById(request.getServiceTypeId())
                    .orElseThrow(() -> new RuntimeException("Dịch vụ vận chuyển không tồn tại"));

            Office fromOffice = null;
            if (request.getFromOfficeId() != null) {
                fromOffice = officePublicService.findById(request.getFromOfficeId())
                        .orElseThrow(() -> new RuntimeException("Bưu cục không tồn tại"));
            }

            boolean existBankAcc = existBankAccount(shopId);
            if (!existBankAcc) {
                return new ApiResponse<>(false,
                        "Bạn cần thêm tài khoản ngân hàng trong hồ sơ cá nhân để nhận tiền COD hoặc thanh toán khi tạo đơn hàng. Vui lòng cập nhật trước khi tiếp tục.",
                        null);
            }

            Integer shippingFee = feeService.calculateShippingFee(
                    weight, request.getServiceTypeId(),
                    senderAddress.getCityCode(), request.getRecipientCityCode());

            System.out.println("reSys" + request.getShippingFee());
            System.out.println("Sys" + shippingFee);

            if (request.getShippingFee() != null && !shippingFee.equals(request.getShippingFee())) {
                return new ApiResponse<>(
                        false,
                        "Phí vận chuyển của đơn vị vận chuyển vừa được cập nhật. Vui lòng kiểm tra lại trước khi tạo đơn.",
                        null);
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
                        .orElseThrow(() -> new RuntimeException("Địa chỉ người nhận không tồn tại"));
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

            return new ApiResponse<>(true, "Tạo đơn hàng thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<UserOrderDetailDto> getOrderByTrackingNumber(int userId, String trackingNumber) {
        try {
            Integer shopId = userUserService.getShopId(userId);
            Order order = repository.findByTrackingNumberAndUserId(trackingNumber, shopId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            List<OrderHistory> orderHistories = orderHistoryRepository
                    .findByOrderIdOrderByActionTimeDesc(order.getId());

            List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());

            UserOrderDetailDto data = OrderMapper.toUserOrderDetailDto(order, orderHistories, orderProducts);

            return new ApiResponse<>(true, "Lấy chi tiết đơn hàng theo mã đơn hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<UserOrderDetailDto> getOrderById(int userId, int id) {
        try {
            Integer shopId = userUserService.getShopId(userId);

            Order order = repository.findByIdAndUserId(id, shopId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            List<OrderHistory> orderHistories = orderHistoryRepository
                    .findByOrderIdOrderByActionTimeDesc(order.getId());

            List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());

            UserOrderDetailDto data = OrderMapper.toUserOrderDetailDto(order, orderHistories, orderProducts);

            return new ApiResponse<>(true, "Lấy chi tiết đơn hàng theo id đơn hàng thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<String> publicOrder(Integer userId, Integer orderId) {
        try {
            Integer shopId = userUserService.getShopId(userId);

            User user = userUserService.getUser(shopId);
            if (user.getLocked()) {
                return new ApiResponse<>(false,
                        "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi chuyển đơn hàng sang xử lý.",
                        null);
            }

            Order order = repository.findByIdAndUserId(orderId, shopId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            if (!OrderUtils.canMoveToPending(order.getStatus())) {
                throw new RuntimeException("Chỉ đơn ở trạng thái 'Nháp' mới được chuyển xử lý");
            }

            List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
            for (OrderProduct op : orderProducts) {
                Product product = op.getProduct();
                if (product.getStatus() != ProductStatus.ACTIVE) {
                    throw new RuntimeException(
                            "Sản phẩm '" + product.getName() + "' đã ngưng bán, không thể tạo đơn");
                }
                if (product.getStock() < op.getQuantity()) {
                    throw new RuntimeException(
                            "Sản phẩm '" + product.getName() + "' vượt quá tồn kho hiện tại (" + product.getStock()
                                    + ")");
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
                    throw new RuntimeException(
                            "Khuyến mãi không còn hiệu lực hoặc bạn không đủ điều kiện sử dụng");
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

            return new ApiResponse<>(true, "Chuyển đơn thành công", trackingNumber);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> cancelOrder(Integer userId, Integer orderId) {
        try {

            Integer shopId = userUserService.getShopId(userId);
            Order order = repository.findByIdAndUserId(orderId, shopId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            if (!OrderUtils.canUserCancel(order.getStatus())) {
                throw new RuntimeException("Đơn hàng đã chuyển sang xử lý, không thể hủy");
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

            return new ApiResponse<>(true, "Hủy đơn hàng thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> setOrderReadyForPickup(Integer userId, Integer orderId) {
        try {

            Integer shopId = userUserService.getShopId(userId);

            Order order = repository.findByIdAndUserId(orderId, shopId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            if (!OrderUtils.canUserSetReady(order.getStatus())) {
                throw new RuntimeException("Trạng thái đơn hàng hiện tại không hợp lệ để chuyển");
            }

            if (!order.getPickupType()
                    .equals(OrderPickupType.PICKUP_BY_COURIER)) {
                throw new RuntimeException("Hình thức lấy hàng của bạn không hợp lệ để chuyển");
            }

            order.setStatus(OrderStatus.READY_FOR_PICKUP);
            repository.save(order);

            orderHistoryUserService.save(
                    order,
                    null,
                    null,
                    null,
                    OrderHistoryActionType.READY_FOR_PICKUP,
                    null);

            return new ApiResponse<>(true, "Đơn hàng đã được chuyển sang trạng thái Sẵn sàng lấy", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> deleteOrder(Integer userId, Integer orderId) {
        try {
            Integer shopId = userUserService.getShopId(userId);
            // Lấy recipientaddress
            Order order = repository.findByIdAndUserId(orderId, shopId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            if (!OrderUtils.canUserDelete(order.getStatus())) {
                throw new RuntimeException("Đơn hàng đã chuyển sang xử lý, không thể xóa");
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

            return new ApiResponse<>(true, "Xóa đơn hàng thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<List<OrderPrintDto>> getOrdersForPrint(Integer userId, List<Integer> orderIds) {
        try {
            Integer shopId = userUserService.getShopId(userId);
            // Lấy danh sách đơn hàng theo userId và orderIds
            List<Order> orders = repository.findByUserIdAndIdIn(shopId, orderIds);

            if (orders.isEmpty()) {
                return new ApiResponse<>(false, "Không tìm thấy đơn hàng nào để in", null);
            }

            // Lọc chỉ những đơn có thể in
            List<Order> printableOrders = orders.stream()
                    .filter(order -> OrderUtils.canUserPrint(order.getStatus()))
                    .toList();

            if (printableOrders.isEmpty()) {
                return new ApiResponse<>(false, "Không có đơn nào đủ điều kiện để in", null);
            }

            // Chuyển sang DTO
            List<OrderPrintDto> printDtos = printableOrders.stream()
                    .map(order -> {
                        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
                        return OrderPrintMapper.toDto(order, orderProducts);
                    })
                    .toList();

            return new ApiResponse<>(true, "Lấy phiếu vận đơn thành công", printDtos);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Boolean> updateOrder(Integer userId, Integer orderId, UserOrderCreateRequest request) {
        try {
            // 1. Validate cơ bản
            validateCreate(request, true);

            Integer shopId = userUserService.getShopId(userId);

            // 2. Lấy recipientaddress
            Order order = repository.findByIdAndUserId(orderId, shopId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            if (!UserOrderEditRuleUtils.canEditUserOrder(order.getStatus())) {
                throw new RuntimeException("Đơn hàng đã hoàn thành không thể chỉnh sửa");
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
                throw new RuntimeException(
                        "Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
            }

            // 4. Cập nhật các field theo rule
            if (request.getSenderAddressId() != null) {
                Address sender = order.getSenderAddress();

                Address reqSenderAddress = addressUserService.findByIdAndUserIdAndType(
                                request.getSenderAddressId(),
                                shopId,
                                AddressType.SENDER)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ người gửi"));

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
                                new RuntimeException("Không tìm thấy địa chỉ người nhận"));

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

                        System.out.println("hello");


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
            updateFieldIfEditable("fromOffice", order.getFromOffice() != null ? order.getFromOffice()
                            .getId() : null,
                    request.getFromOfficeId(), currentStatus,
                    val -> order.setFromOffice(val != null ? officePublicService.findById(val)
                            .orElseThrow() : null));
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
                            .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));

                    if (!promotionUserService.canUsePromotion(shopId, promotion.getId(), order.getServiceType()
                                    .getId(),
                            order.getShippingFee(), order.getWeight())) {
                        throw new RuntimeException("Bạn không đủ điều kiện để dùng mã giảm giá");
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
            return new ApiResponse<>(true, "Cập nhật đơn hàng thành công", true);
        } catch (

                Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
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
            throw new RuntimeException("Lỗi khi xuất Excel", e);
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
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + item.getProductId()));

            if (!product.getUser()
                    .getId()
                    .equals(shopId))
                throw new RuntimeException("Sản phẩm " + product.getName() + " không thuộc về bạn");

            if (product.getStatus() != ProductStatus.ACTIVE)
                throw new RuntimeException("Sản phẩm " + product.getName() + " đã ngưng bán.");

            OrderProduct existing = existingMap.get(product.getId());
            int oldQty = existing != null ? existing.getQuantity() : 0;
            int stockAvailable = product.getStock() + (adjustStock && existing != null ? oldQty : 0);

            if (item.getQuantity() > stockAvailable)
                throw new RuntimeException(
                        "Sản phẩm " + product.getName() + " vượt quá tồn kho (" + stockAvailable + ")");

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
            ,OrderStatus currentStatus,
            Consumer<T> setter) {

        if (OrderFieldUtils.isChanged(oldValue, newValue)) {
            var rule = UserOrderEditRuleUtils.USER_ORDER_FIELD_EDIT_RULES.get(fieldName);
            if (rule != null) {
                // Nếu nonEditableStatuses chứa currentStatus → không được sửa
                if (rule.getNonEditableStatuses() != null && !rule.getNonEditableStatuses().isEmpty()) {
                    if (rule.getNonEditableStatuses().contains(currentStatus)) {
                        throw new RuntimeException("Trường '" + fieldName + "' không thể thay đổi khi ở trạng "
                                + "thái " + currentStatus);
                    }
                }

                // Nếu editableStatuses != null → chỉ cho phép những trạng thái đó
                if (rule.getEditableStatuses() != null && !rule.getEditableStatuses().isEmpty()) {
                    if (!rule.getEditableStatuses().contains(currentStatus)) {
                        throw new RuntimeException("Trường '" + fieldName + "' không thể thay đổi khi ở trạng "
                                + "thái " + currentStatus);
                    }
                }
                // Nếu editableStatuses = null → mặc định tất cả trạng thái OK trừ
                // nonEditableStatuses
            }

            setter.accept(newValue);
        }
    }

    private void validateBeforePublish(Integer userId, UserOrderCreateRequest request) {

        User user = userUserService.getUser(userId);
        if (user.getLocked()) {
            throw new RuntimeException(
                    "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi chuyển đơn hàng sang xử lý.");
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
                    .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));

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
                throw new RuntimeException("Bạn không đủ điều kiện để dùng mã giảm giá");
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
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missing));
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
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ");
        }

        try {
            OrderPickupType.valueOf(request.getPickupType());
        } catch (Exception e) {
            throw new RuntimeException("Hình thức giao hàng không hợp lệ");
        }

        try {
            OrderPayerType.valueOf(request.getPayer());
        } catch (Exception e) {
            throw new RuntimeException("Người trả phí không hợp lệ");
        }

        if (request.getSenderAddressId() != null && request.getSenderAddressId() <= 0)
            throw new RuntimeException("Mã địa chỉ người gửi không hợp lệ");

        if (!isNewRecipient && request.getRecipientAddressId() != null && request.getRecipientAddressId() <= 0)
            throw new RuntimeException("Mã địa chỉ người nhận không hợp lệ");

        if (!request.getRecipientPhone()
                .matches("\\d{10}"))
            throw new RuntimeException("Số điện thoại người nhận phải gồm đúng 10 chữ số");

        if (isNewRecipient && request.getRecipientCityCode() <= 0)
            throw new RuntimeException("Mã Thành phố không hợp lệ");
        if (isNewRecipient && request.getRecipientWardCode() <= 0)
            throw new RuntimeException("Mã Phường/Xã không hợp lệ");

        if (isNewRecipient) {
            Double lat = request.getRecipientLatitude();
            Double lng = request.getRecipientLongitude();

            if (lat == null || lng == null
                    || lat < -90 || lat > 90
                    || lng < -180 || lng > 180) {
                throw new RuntimeException("Tọa độ không hợp lệ");
            }
        }

        if (request.getWeight()
                .doubleValue() <= 0)
            throw new RuntimeException("Khối lượng quy đổi phải lớn hơn 0");

        if (request.getOriginalWeight()
                .doubleValue() <= 0)
            throw new RuntimeException("Khối lượng thực tế phải lớn hơn 0");

        if (request.getHeight()
                .doubleValue() <= 0)
            throw new RuntimeException("Chiều cao phải lớn hơn 0");

        if (request.getLength()
                .doubleValue() <= 0)
            throw new RuntimeException("Chiều dài phải lớn hơn 0");

        if (request.getWidth()
                .doubleValue() <= 0)
            throw new RuntimeException("Chiều rộng phải lớn hơn 0");

        if (request.getServiceTypeId() <= 0)
            throw new RuntimeException("Mã dịch vụ không hợp lệ");

        if (request.getCod() < 0)
            throw new RuntimeException("Phí thu hộ không hợp lệ");

        if (request.getOrderValue() != null && request.getOrderValue() < 0)
            throw new RuntimeException("Giá trị đơn hàng phải lớn hơn hoặc bằng 0");

        if (request.getNotes() != null && request.getNotes()
                .length() > 1000)
            throw new RuntimeException("Ghi chú tối đa 1000 ký tự");

        if (request.getPromotionId() != null && request.getPromotionId() <= 0)
            throw new RuntimeException("Mã khuyến mãi không hợp lệ");

        if (OrderPickupType.AT_OFFICE.name()
                .equals(request.getPickupType())) {
            if (request.getFromOfficeId() == null)
                throw new RuntimeException("Bưu cục nhận hàng không được để trống");
        }

        if (request.getOrderProducts() != null) {
            int index = 1;
            for (var op : request.getOrderProducts()) {
                if (op.getProductId() == null || op.getProductId() <= 0)
                    throw new RuntimeException("Sản phẩm thứ " + index + " không hợp lệ (productId)");
                if (op.getQuantity() == null || op.getQuantity() <= 0)
                    throw new RuntimeException("Sản phẩm thứ " + index + " có số lượng không hợp lệ");
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
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + item.getProductId()));

            if (!product.getUser()
                    .getId()
                    .equals(userId)) {
                throw new RuntimeException("Sản phẩm không thuộc user: " + product.getName());
            }

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " đã bị vô hiệu hóa");
            }

            if (product.getStock() <= 0) {
                throw new RuntimeException("Sản phẩm " + product.getName() + " đã hết hàng");
            }

            if (item.getQuantity() > product.getStock()) {
                throw new RuntimeException("Số lượng " + product.getName() +
                        " vượt quá tồn kho (" + product.getStock() + ")");
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
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + item.getProductId()));

            if (!isDraft) {
                int newStock = product.getStock() - item.getQuantity();
                if (newStock < 0)
                    throw new RuntimeException("Sản phẩm '" + product.getName() + "' vượt quá tồn kho");
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
}
