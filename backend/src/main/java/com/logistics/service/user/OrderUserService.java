package com.logistics.service.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
import com.logistics.enums.OrderCreatorType;
import com.logistics.enums.OrderHistoryActionType;
import com.logistics.enums.OrderPayerType;
import com.logistics.enums.OrderPaymentStatus;
import com.logistics.enums.OrderPickupType;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.ProductStatus;
import com.logistics.mapper.OrderMapper;
import com.logistics.mapper.OrderPrintMapper;
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
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<OrderCreateSuccess> create(Integer userId, UserOrderCreateRequest request) {
        try {
            validateCreate(request);

            if (!addressUserService.checkAddressBelongsToUser(request.getSenderAddressId(), userId)) {
                return new ApiResponse<>(true, "Địa chỉ người nhận không thuộc người dùng", null);
            }

            if (request.getFromOfficeId() != null) {
                if (!officePublicService.isSameCity(request.getSenderAddressId(), request.getFromOfficeId())) {
                    return new ApiResponse<>(true, "Địa chỉ gửi và bưu cục phải thuộc cùng thành phố", null);
                }
            }

            if (!serviceTypeUserService.serviceTypeExists(request.getServiceTypeId())) {
                return new ApiResponse<>(true, "Dịch vụ vận chuyển không tồn tại", null);
            }

            if (request.getOrderProducts() != null && !request.getOrderProducts().isEmpty()) {
                validateProductWithDB(userId, request.getOrderProducts());
            }

            Address senderAddress = addressUserService.findById(request.getSenderAddressId())
                    .orElseThrow(() -> new RuntimeException("Địa chỉ người gửi không tồn tại"));

            Integer serviceFee = feeService.calculateTotalFee(request.getWeight(), request.getServiceTypeId(),
                    senderAddress.getCityCode(), request.getRecipientCityCode(),
                    request.getOrderValue(), request.getCod());

            System.out.println("serviceFee" + serviceFee);

            Promotion promotion = null;
            int discountAmount = 0;

            if (request.getPromotionId() != null && request.getPromotionId() > 0) {
                promotion = promotionUserService.findById(request.getPromotionId())
                        .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));

                if (!promotionUserService.canUsePromotion(
                        userId,
                        request.getPromotionId(),
                        request.getServiceTypeId(),
                        serviceFee,
                        request.getWeight())) {
                    return new ApiResponse<>(false, "Bạn không đủ điều kiện để dùng mã giảm giá", null);
                }

                discountAmount = promotionUserService.calculateDiscount(promotion, serviceFee);
            }

            System.out.println("discountAmount" + discountAmount);

            User user = userUserService.findById(userId);

            ServiceType serviceType = serviceTypeUserService.findById(request.getServiceTypeId())
                    .orElseThrow(() -> new RuntimeException("Dịch vụ vận chuyển không tồn tại"));

            Office fromOffice = null;
            if (request.getFromOfficeId() != null) {
                fromOffice = officePublicService.findById(request.getFromOfficeId())
                        .orElseThrow(() -> new RuntimeException("Bưu cục không tồn tại"));
            }

            Integer shippingFee = feeService.calculateShippingFee(
                    request.getWeight(), request.getServiceTypeId(),
                    senderAddress.getCityCode(), request.getRecipientCityCode());
            System.out.println("ShippingFee" + shippingFee);

            Integer totalFee = serviceFee - discountAmount;
            System.out.println("Total Fee" + totalFee);

            Integer orderValue = calculateOrderValue(request.getOrderProducts());

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

            Order newOrder = repository.save(order);

            saveOrderProducts(newOrder, request.getOrderProducts());

            promotionUserService.increaseUsage(request.getPromotionId(), userId);

            if (newOrder.getStatus() == OrderStatus.PENDING) {
                orderHistoryUserService.save(newOrder, null, null,
                        null, OrderHistoryActionType.PENDING, null);
            }

            OrderCreateSuccess result = new OrderCreateSuccess();
            result.setOrderId(newOrder.getId());
            result.setTrackingNumber(newOrder.getTrackingNumber());

            return new ApiResponse<>(true, "Tạo đơn hàng thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
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
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
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
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<String> publicOrder(Integer userId, Integer orderId) {
        Order order = repository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!OrderUtils.canMoveToPending(order.getStatus())) {
            throw new RuntimeException("Chỉ đơn ở trạng thái 'Nháp' mới được chuyển xử lý");
        }

        order.setStatus(OrderStatus.PENDING);

        String trackingNumber = generateUniqueTrackingNumber(order.getStatus());
        order.setTrackingNumber(trackingNumber);
        repository.save(order);

        if (order.getStatus() == OrderStatus.PENDING) {
            orderHistoryUserService.save(order, null, null,
                    null, OrderHistoryActionType.PENDING, null);
        }

        return new ApiResponse<>(true, "Chuyển đơn thành công", trackingNumber);
    }

    public ApiResponse<Boolean> cancelOrder(Integer userId, Integer orderId) {

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
    }

    @Transactional
    public ApiResponse<Boolean> deleteOrder(Integer userId, Integer orderId) {
        // Lấy order
        Order order = repository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!OrderUtils.canUserDelete(order.getStatus())) {
            throw new RuntimeException("Đơn hàng đã chuyển sang xử lý, không thể xóa");
        }

        List<OrderProduct> orderProducts = orderProductRepository.findByOrderId(order.getId());
        productUserService.restoreStockFromOrder(orderProducts);

        if (order.getPromotion() != null) {
            promotionUserService.decreaseUsage(order.getPromotion().getId(), userId);
        }

        List<OrderHistory> histories = orderHistoryRepository.findByOrderId(order.getId());
        if (!histories.isEmpty()) {
            orderHistoryRepository.deleteAll(histories);
        }

        if (!orderProducts.isEmpty()) {
            orderProductRepository.deleteAll(orderProducts);
        }

        Address recipientAddress = order.getRecipientAddress();
        if (recipientAddress != null) {
            addressUserService.delete(recipientAddress);
        }

        repository.delete(order);

        return new ApiResponse<>(true, "Xóa đơn hàng thành công", true);
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
                        return OrderPrintMapper.toDto(order, orderProducts); // truyền đúng kiểu
                    })
                    .toList();

            return new ApiResponse<>(true, "Lấy phiếu vận đơn thành công", printDtos);

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy phiếu vận đơn: " + e.getMessage(), null);
        }
    }

    private void validateCreate(UserOrderCreateRequest request) {

        List<String> missing = new ArrayList<>();

        if (isBlank(request.getStatus()))
            missing.add("Trạng thái đơn hàng");
        if (request.getSenderAddressId() == null)
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

        if (request.getSenderAddressId() <= 0)
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
                throw new RuntimeException("Sản phẩm bị vô hiệu hóa: " + product.getName());
            }

            if (product.getStock() <= 0) {
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' đã hết hàng");
            }

            if (item.getQuantity() > product.getStock()) {
                throw new RuntimeException("Số lượng '" + product.getName() +
                        "' vượt quá tồn kho (" + product.getStock() + ")");
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

    public int calculateOrderValue(List<UserOrderCreateRequest.OrderProduct> items) {
        if (items == null || items.isEmpty()) {
            return 0;
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

        for (var item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + item.getProductId()));

            int newStock = product.getStock() - item.getQuantity();
            if (newStock < 0)
                throw new RuntimeException("Sản phẩm '" + product.getName() + "' vượt quá tồn kho");
            product.setStock(newStock);
            product.setSoldQuantity(product.getSoldQuantity() + item.getQuantity());
            productRepository.save(product);

            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setOrder(order);
            orderProduct.setProduct(product);
            orderProduct.setQuantity(item.getQuantity());
            orderProduct.setPrice(product.getPrice());
            orderProductRepository.save(orderProduct);
        }
    }
}
