package com.logistics.service.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
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

@Service
public class OrderUserService {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private PromotionUserService promotionUserService;

    @Autowired
    private AddressUserService addressUserService;

    @Autowired
    private ServiceTypeUserService serviceTypeUserService;

    @Autowired
    private OfficePublicService officePublicService;

    @Autowired
    private FeePublicService feeService;

    @Autowired
    private UserUserService userUserService;

    @Autowired
    private ProductUserService productUserService;

    @Autowired
    private OrderHistoryUserService orderHistoryUserService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    public ApiResponse<ListResponse<UserOrderListDto>> list(int userId, UserOrderSearchRequest request) {
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

            Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                    .and(OrderSpecification.userId(userId))
                    .and(OrderSpecification.search(search))
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

            Specification<Order> spec = OrderSpecification.unrestrictedOrder()
                    .and(OrderSpecification.userId(userId))
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

            User user = userUserService.findById(userId);
            if (user.getLocked()) {
                return new ApiResponse<>(false,
                        "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới.",
                        null);
            }

            if (!addressUserService.checkAddressBelongsToUser(request.getSenderAddressId(), userId)) {
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

            if (request.getOrderProducts() != null && !request.getOrderProducts().isEmpty()) {
                validateProductWithDB(userId, request.getOrderProducts());
            }

            Address senderAddress = addressUserService.findById(request.getSenderAddressId())
                    .orElseThrow(() -> new RuntimeException("Địa chỉ người gửi không tồn tại"));

            BigDecimal weight = calculateWeight(request.getOrderProducts(), request.getWeight());

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
                        userId,
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

            boolean existBankAcc = existBankAccount(userId);
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
            order.setStatus(OrderStatus.valueOf(request.getStatus()));
            order.setTrackingNumber(generateUniqueTrackingNumber(order.getStatus()));
            order.setCreatedByType(OrderCreatorType.USER);
            order.setUser(user);
            order.setSenderName(senderAddress.getName());
            order.setSenderPhone(senderAddress.getPhoneNumber());
            order.setSenderCityCode(senderAddress.getCityCode());
            order.setSenderWardCode(senderAddress.getWardCode());
            order.setSenderDetail(senderAddress.getDetail());
            order.setSenderAddress(senderAddress);
            order.setRecipientName(request.getRecipientName());
            order.setRecipientPhone(request.getRecipientPhone());
            order.setRecipientAddress(recipientAddress);
            order.setPickupType(OrderPickupType.valueOf(request.getPickupType()));
            order.setWeight(request.getWeight());
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

            Order newOrder = repository.save(order);

            saveOrderProducts(newOrder, request.getOrderProducts());

            boolean isDraft = OrderStatus.DRAFT.name().equals(request.getStatus());

            if (!isDraft) {
                orderHistoryUserService.save(newOrder, null, null,
                        null, OrderHistoryActionType.PENDING, null);

                if (promotion != null) {
                    promotionUserService.increaseUsage(request.getPromotionId(), userId);
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
            Order order = repository.findByTrackingNumberAndUserId(trackingNumber, userId)
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
            Order order = repository.findByIdAndUserId(id, userId)
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

            User user = userUserService.findById(userId);
            if (user.getLocked()) {
                return new ApiResponse<>(false,
                        "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi chuyển đơn hàng sang xử lý.",
                        null);
            }

            Order order = repository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            if (!OrderUtils.canMoveToPending(order.getStatus())) {
                throw new RuntimeException("Chỉ đơn ở trạng thái 'Nháp' mới được chuyển xử lý");
            }

            List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
            for (OrderProduct op : orderProducts) {
                Product product = op.getProduct();
                if (product.getStatus() != ProductStatus.ACTIVE) {
                    throw new RuntimeException("Sản phẩm '" + product.getName() + "' đã ngưng bán, không thể tạo đơn");
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
                        userId,
                        promotion.getId(),
                        order.getServiceType().getId(),
                        order.getShippingFee(),
                        order.getWeight());
                if (!canUse) {
                    throw new RuntimeException("Khuyến mãi không còn hiệu lực hoặc bạn không đủ điều kiện sử dụng");
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
                promotionUserService.increaseUsage(promotion.getId(), userId);
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

            Order order = repository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            if (!OrderUtils.canUserCancel(order.getStatus())) {
                throw new RuntimeException("Đơn hàng đã chuyển sang xử lý, không thể hủy");
            }

            order.setStatus(OrderStatus.CANCELLED);
            repository.save(order);

            List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
            productUserService.restoreStockFromOrder(orderProducts);

            if (order.getPromotion() != null) {
                promotionUserService.decreaseUsage(order.getPromotion().getId(), userId);
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

            Order order = repository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            if (!OrderUtils.canUserSetReady(order.getStatus())) {
                throw new RuntimeException("Trạng thái đơn hàng hiện tại không hợp lệ để chuyển");
            }

            if (!order.getPickupType().equals(OrderPickupType.PICKUP_BY_COURIER)) {
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
            // Lấy order
            Order order = repository.findByIdAndUserId(orderId, userId)
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

            Address recipientAddress = order.getRecipientAddress();
            if (recipientAddress != null) {
                addressUserService.delete(recipientAddress);
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
            // Lấy danh sách đơn hàng theo userId và orderIds
            List<Order> orders = repository.findByUserIdAndIdIn(userId, orderIds);

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

            // 2. Lấy order
            Order order = repository.findByIdAndUserId(orderId, userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

            if (!UserOrderEditRuleUtils.canEditUserOrder(order.getStatus())) {
                throw new RuntimeException("Đơn hàng đã hoàn thành không thể chỉnh sửa");
            }

            OrderStatus currentStatus = order.getStatus();
            OrderStatus newStatus = OrderStatus.valueOf(request.getStatus());
            boolean movingDraftToPending = currentStatus == OrderStatus.DRAFT && newStatus == OrderStatus.PENDING;

            // 3. Chuyển trạng thái nếu từ DRAFT sang PENDING
            if (movingDraftToPending) {
                validateBeforePublish(userId, request);
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
                throw new RuntimeException("Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
            }

            // 4. Cập nhật các field theo rule
            if (request.getSenderAddressId() != null) {
                Address sender = order.getSenderAddress();

                if (!addressUserService.checkAddressBelongsToUser(request.getSenderAddressId(), userId)) {
                    return new ApiResponse<>(false, "Địa chỉ người gửi không thuộc người dùng", null);
                }

                Address reqSenderAddress = addressUserService.findById(request.getSenderAddressId())
                        .orElseThrow(() -> new RuntimeException("Địa chỉ người gửi không tồn tại"));

                // Cập nhật toàn bộ object senderAddress
                updateFieldIfEditable("senderAddress", sender, reqSenderAddress, order,
                        currentStatus, order::setSenderAddress);

                // Cập nhật từng field để giữ consistency nếu cần
                updateFieldIfEditable("senderName", sender.getName(), reqSenderAddress.getName(), order,
                        currentStatus, order::setSenderName);
                updateFieldIfEditable("senderPhoneNumber", sender.getPhoneNumber(),
                        reqSenderAddress.getPhoneNumber(), order,
                        currentStatus, order::setSenderPhone);
                updateFieldIfEditable("senderCityCode", sender.getCityCode(),
                        reqSenderAddress.getCityCode(), order,
                        currentStatus, order::setSenderCityCode);
                updateFieldIfEditable("senderWardCode", sender.getWardCode(),
                        reqSenderAddress.getWardCode(), order,
                        currentStatus, order::setSenderWardCode);
                updateFieldIfEditable("senderDetailAddress", sender.getDetail(),
                        reqSenderAddress.getDetail(), order,
                        currentStatus, order::setSenderDetail);
            }
            Address recipient = order.getRecipientAddress();
            updateFieldIfEditable("recipientName", order.getRecipientName(), request.getRecipientName(), order,
                    currentStatus, order::setRecipientName);
            updateFieldIfEditable("recipientName", recipient.getName(), request.getRecipientName(), order,
                    currentStatus, recipient::setName);
            updateFieldIfEditable("recipientPhoneNumber", order.getRecipientPhone(), request.getRecipientPhone(), order,
                    currentStatus, order::setRecipientPhone);
            updateFieldIfEditable("recipientPhoneNumber", recipient.getPhoneNumber(), request.getRecipientPhone(),
                    order,
                    currentStatus, recipient::setPhoneNumber);
            updateFieldIfEditable("recipientCityCode", recipient.getCityCode(), request.getRecipientCityCode(), order,
                    currentStatus, recipient::setCityCode);
            updateFieldIfEditable("recipientWardCode", recipient.getWardCode(), request.getRecipientWardCode(), order,
                    currentStatus, recipient::setWardCode);
            updateFieldIfEditable("recipientDetailAddress", recipient.getDetail(), request.getRecipientDetail(), order,
                    currentStatus, recipient::setDetail);
            updateFieldIfEditable("pickupType", order.getPickupType().name(), request.getPickupType(), order,
                    currentStatus,
                    val -> order.setPickupType(OrderPickupType.valueOf(val)));
            updateFieldIfEditable("fromOffice", order.getFromOffice() != null ? order.getFromOffice().getId() : null,
                    request.getFromOfficeId(), order, currentStatus,
                    val -> order.setFromOffice(val != null ? officePublicService.findById(val).orElseThrow() : null));
            updateFieldIfEditable("payer", order.getPayer().name(), request.getPayer(), order, currentStatus,
                    val -> order.setPayer(OrderPayerType.valueOf(val)));
            updateFieldIfEditable("notes", order.getNotes(), request.getNotes(), order, currentStatus, order::setNotes);

            // 5. Cập nhật products nếu có và được phép
            if (request.getOrderProducts() != null) {
                if (UserOrderEditRuleUtils.canEditUserOrderField("products", currentStatus)) {
                    updateOrderProductsWithValidation(userId, order, request.getOrderProducts(), movingDraftToPending);
                }
            }

            // 6. Cập nhật weight và orderValue chỉ khi được phép
            BigDecimal calcWeight = calculateWeight(request.getOrderProducts(), request.getWeight());
            int calcOrderValue = calculateOrderValue(request.getOrderProducts(), request.getOrderValue());

            System.out.println("weightRe" + calcWeight);
            System.out.println("weightsYS" + order.getWeight());

            updateFieldIfEditable("weight", order.getWeight(), calcWeight, order, currentStatus, order::setWeight);
            updateFieldIfEditable("orderValue", order.getOrderValue(), calcOrderValue, order, currentStatus,
                    order::setOrderValue);

            // 7. Áp dụng promotion nếu được phép
            if (UserOrderEditRuleUtils.canEditUserOrderField("promotion", currentStatus)) {
                Integer oldPromotionId = order.getPromotion() != null ? order.getPromotion().getId() : null;
                Integer newPromotionId = request.getPromotionId();

                boolean adjustPromotion = movingDraftToPending || order.getStatus() != OrderStatus.DRAFT;

                if (oldPromotionId != null && !Objects.equals(oldPromotionId, newPromotionId) && adjustPromotion) {
                    promotionUserService.decreaseUsage(oldPromotionId, userId);
                }

                if (newPromotionId != null) {
                    Promotion promotion = promotionUserService.findById(newPromotionId)
                            .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));

                    if (!promotionUserService.canUsePromotion(userId, promotion.getId(), order.getServiceType().getId(),
                            order.getShippingFee(), order.getWeight())) {
                        throw new RuntimeException("Bạn không đủ điều kiện để dùng mã giảm giá");
                    }

                    order.setPromotion(promotion);
                    order.setDiscountAmount(promotionUserService.calculateDiscount(promotion, order.getShippingFee()));

                    if (adjustPromotion) {
                        promotionUserService.increaseUsage(promotion.getId(), userId);
                    }
                } else {
                    order.setPromotion(null);
                    order.setDiscountAmount(0);
                }
            }

            // 8. Cập nhật shippingFee dựa trên weight và serviceType
            int calcShippingFee = feeService.calculateShippingFee(calcWeight, order.getServiceType().getId(),
                    order.getSenderCityCode(), order.getRecipientAddress().getCityCode());
            updateFieldIfEditable("shippingFee", order.getShippingFee(), calcShippingFee, order, currentStatus,
                    order::setShippingFee);

            // 9. Cập nhật totalFee = shippingFee - discountAmount
            int calcServiceFee = feeService.calculateTotalFee(
                    calcWeight,
                    order.getServiceType().getId(),
                    order.getSenderCityCode(),
                    order.getRecipientAddress().getCityCode(),
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

    @Transactional
    private void updateOrderProductsWithValidation(Integer userId, Order order,
            List<UserOrderCreateRequest.OrderProduct> items, boolean movingDraftToPending) {

        List<OrderProduct> existingProducts = orderProductRepository.findByOrderId(order.getId());
        boolean adjustStock = movingDraftToPending || order.getStatus() != OrderStatus.DRAFT;

        // --- XỬ LÝ TRƯỜNG HỢP LIST RỖNG ---
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
            return; // Xóa xong thì thoát hàm
        }

        // --- Phần update bình thường ---
        Map<Integer, OrderProduct> existingMap = new HashMap<>();
        for (OrderProduct op : existingProducts)
            existingMap.put(op.getProduct().getId(), op);

        for (var item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + item.getProductId()));

            if (!product.getUser().getId().equals(userId))
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
            if (items.stream().noneMatch(i -> i.getProductId().equals(oldOp.getProduct().getId()))) {
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

    private <T> void updateFieldIfEditable(String fieldName, T oldValue, T newValue,
            Order order, OrderStatus currentStatus, java.util.function.Consumer<T> setter) {

        boolean changed;

        if (oldValue instanceof List<?> oldList && newValue instanceof List<?> newList) {
            changed = !listsEqual(oldList, newList);
        } else if (oldValue instanceof Set<?> oldSet && newValue instanceof Set<?> newSet) {
            changed = !setsEqual(oldSet, newSet);
        } else if (oldValue instanceof Map<?, ?> oldMap && newValue instanceof Map<?, ?> newMap) {
            changed = !mapsEqual(oldMap, newMap);
        } else if (oldValue instanceof BigDecimal oldBd && newValue instanceof BigDecimal newBd) {
            // So sánh BigDecimal bằng compareTo để bỏ qua scale
            changed = oldBd.compareTo(newBd) != 0;
        } else {
            changed = !Objects.equals(oldValue, newValue);
        }

        if (changed) {
            var rule = UserOrderEditRuleUtils.USER_ORDER_FIELD_EDIT_RULES.get(fieldName);
            if (rule != null) {
                // Nếu nonEditableStatuses chứa currentStatus → không được sửa
                if (rule.getNonEditableStatuses() != null && rule.getNonEditableStatuses().contains(currentStatus)) {
                    throw new RuntimeException(
                            "Trường '" + fieldName + "' không thể thay đổi khi đơn ở trạng thái " + currentStatus);
                }
                // Nếu editableStatuses != null → chỉ cho phép những trạng thái đó
                if (rule.getEditableStatuses() != null && !rule.getEditableStatuses().contains(currentStatus)) {
                    throw new RuntimeException(
                            "Trường '" + fieldName + "' không thể thay đổi khi đơn ở trạng thái " + currentStatus);
                }
                // Nếu editableStatuses = null → mặc định tất cả trạng thái OK trừ
                // nonEditableStatuses
            }

            setter.accept(newValue);
        }
    }

    // Hàm so sánh List
    private boolean listsEqual(List<?> a, List<?> b) {
        if (a == null)
            a = List.of();
        if (b == null)
            b = List.of();
        return a.equals(b);
    }

    private boolean setsEqual(Set<?> oldSet, Set<?> newSet) {
        if (oldSet == null && newSet == null)
            return true;
        if (oldSet == null || newSet == null)
            return false;
        return oldSet.equals(newSet);
    }

    private boolean mapsEqual(Map<?, ?> oldMap, Map<?, ?> newMap) {
        if (oldMap == null && newMap == null)
            return true;
        if (oldMap == null || newMap == null)
            return false;
        return oldMap.equals(newMap);
    }

    // --- Hàm validate trước khi publish DRAFT sang PENDING ---
    private void validateBeforePublish(Integer userId, UserOrderCreateRequest request) {

        User user = userUserService.findById(userId);
        if (user.getLocked()) {
            throw new RuntimeException(
                    "Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi chuyển đơn hàng sang xử lý.");
        }

        // Kiểm tra các field bắt buộc
        validateCreate(request, true);

        // Kiểm tra sản phẩm
        if (request.getOrderProducts() != null && !request.getOrderProducts().isEmpty()) {
            validateProductWithDB(userId, request.getOrderProducts());
        }

        // Kiểm tra promotion nếu có
        if (request.getPromotionId() != null) {
            Promotion promotion = promotionUserService.findById(request.getPromotionId())
                    .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));

            if (!promotionUserService.canUsePromotion(userId, promotion.getId(),
                    request.getServiceTypeId(), request.getShippingFee(),
                    calculateWeight(request.getOrderProducts(), null))) {
                throw new RuntimeException("Bạn không đủ điều kiện để dùng mã giảm giá");
            }
        }
    }

    @Transactional
    private void updateOrderProducts(Order order, List<UserOrderCreateRequest.OrderProduct> items) {
        // Lấy danh sách product cũ
        List<OrderProduct> existingProducts = orderProductRepository.findByOrderId(order.getId());

        // Tạo map để dễ lookup
        Map<Integer, Integer> existingQuantityMap = new HashMap<>();
        for (OrderProduct op : existingProducts) {
            existingQuantityMap.put(op.getProduct().getId(), op.getQuantity());
        }

        for (var item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + item.getProductId()));

            // Kiểm tra trạng thái sản phẩm
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new RuntimeException("Sản phẩm '" + product.getName()
                        + "' hiện tại đã ngưng bán và không thể cập nhật số lượng. Vui lòng loại bỏ sản phẩm này khỏi đơn hàng.");
            }

            int oldQuantity = existingQuantityMap.getOrDefault(product.getId(), 0);
            int stockAvailable = product.getStock() + oldQuantity;

            if (item.getQuantity() > stockAvailable) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' vượt quá tồn kho hiện tại ("
                        + stockAvailable + "). Vui lòng điều chỉnh số lượng.");
            }

            // Cập nhật stock và soldQuantity
            int delta = item.getQuantity() - oldQuantity;
            product.setStock(product.getStock() - delta);
            product.setSoldQuantity(product.getSoldQuantity() + delta);
            productRepository.save(product);

            // Lưu orderProduct
            OrderProduct orderProduct = existingProducts.stream()
                    .filter(op -> op.getProduct().getId().equals(product.getId()))
                    .findFirst()
                    .orElse(new OrderProduct());

            orderProduct.setOrder(order);
            orderProduct.setProduct(product);
            orderProduct.setQuantity(item.getQuantity());
            orderProduct.setPrice(product.getPrice());
            orderProductRepository.save(orderProduct);
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
        if (isBlank(request.getRecipientName()))
            missing.add("Tên người nhận");
        if (isBlank(request.getRecipientPhone()))
            missing.add("Số điện thoại người nhận");
        if (request.getRecipientCityCode() == null)
            missing.add("Tỉnh/Thành người nhận");
        if (request.getRecipientWardCode() == null)
            missing.add("Phường/Xã người nhận");
        if (isBlank(request.getRecipientDetail()))
            missing.add("Địa chỉ chi tiết người nhận");
        if (isBlank(request.getPickupType()))
            missing.add("Hình thức giao hàng");
        if (request.getWeight() == null)
            missing.add("Khối lượng");
        if (request.getServiceTypeId() == null)
            missing.add("Loại dịch vụ");
        if (request.getCod() == null)
            missing.add("Phí thu hộ");

        if ((request.getOrderProducts() == null || request.getOrderProducts().isEmpty())
                && request.getOrderValue() == null)
            missing.add("Giá trị đơn hàng");

        if (isBlank(request.getPayer()))
            missing.add("Người trả phí");

        if (!missing.isEmpty()) {
            throw new RuntimeException("Thiếu thông tin: " + String.join(", ", missing));
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

        if (!request.getRecipientPhone().matches("\\d{10}"))
            throw new RuntimeException("Số điện thoại người nhận phải gồm đúng 10 chữ số");

        if (request.getRecipientCityCode() <= 0)
            throw new RuntimeException("Mã Thành phố không hợp lệ");
        if (request.getRecipientWardCode() <= 0)
            throw new RuntimeException("Mã Phường/Xã không hợp lệ");

        if (request.getWeight().doubleValue() <= 0)
            throw new RuntimeException("Khối lượng phải lớn hơn 0");

        if (request.getServiceTypeId() <= 0)
            throw new RuntimeException("Mã dịch vụ không hợp lệ");

        if (request.getCod() < 0)
            throw new RuntimeException("Phí thu hộ không hợp lệ");

        if (request.getOrderValue() != null && request.getOrderValue() < 0)
            throw new RuntimeException("Giá trị đơn hàng phải lớn hơn hoặc bằng 0");

        if (request.getNotes() != null && request.getNotes().length() > 1000)
            throw new RuntimeException("Ghi chú tối đa 1000 ký tự");

        if (request.getPromotionId() != null && request.getPromotionId() <= 0)
            throw new RuntimeException("Mã khuyến mãi không hợp lệ");

        if (OrderPickupType.AT_OFFICE.name().equals(request.getPickupType())) {
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

            if (!product.getUser().getId().equals(userId)) {
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

    public BigDecimal calculateWeight(List<UserOrderCreateRequest.OrderProduct> items, BigDecimal weight) {
        if (items == null || items.isEmpty()) {
            return weight != null ? weight : BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var item : items) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow();
            BigDecimal itemWeight = product.getWeight().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemWeight);
        }

        return total;
    }

    public int calculateOrderValue(List<UserOrderCreateRequest.OrderProduct> items, Integer orderValue) {
        if (items == null || items.isEmpty()) {
            return orderValue != null ? orderValue : 0;
        }

        int total = 0;
        for (var item : items) {
            Product product = productRepository.findById(item.getProductId()).orElseThrow();
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
