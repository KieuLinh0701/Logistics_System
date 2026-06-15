package com.logistics.service.user;

import com.logistics.entity.Address;
import com.logistics.entity.Order;
import com.logistics.entity.ShippingRequest;
import com.logistics.entity.User;
import com.logistics.enums.AddressType;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.RecipientAddressType;
import com.logistics.mapper.AddressMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.recipientaddress.RecipientAddressUserRequest;
import com.logistics.request.user.recipientaddress.RecipientSuggestionRequest;
import com.logistics.request.user.recipientaddress.UserRecipientAddressSearchRequest;
import com.logistics.request.user.shippingRequest.UserShippingRequestSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.response.user.recipientaddress.RecipientAddress;
import com.logistics.response.user.recipientaddress.RecipientAddressResponse;
import com.logistics.response.user.recipientaddress.RecipientAddressWithStats;
import com.logistics.response.user.recipientaddress.RecipientStats;
import com.logistics.response.user.recipientaddress.RecipientSuggestionAddressResponse;
import com.logistics.specification.RecipientAddressSpecification;
import com.logistics.specification.ShippingRequestSpecification;
import com.logistics.utils.AddressUtils;
import lombok.RequiredArgsConstructor;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.logistics.utils.ShippingRequestUtils.translateShippingRequestStatus;
import static com.logistics.utils.ShippingRequestUtils.translateShippingRequestType;

@Service
@RequiredArgsConstructor
public class RecipientAddressUserService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final UserUserService userService;

    public ApiResponse<ListResponse<RecipientAddressResponse>> list(int userId, UserRecipientAddressSearchRequest request) {
        try {
            Integer shopId = userService.getShopId(userId);

            int page = request.getPage();
            int limit = request.getLimit();
            String keyword = request.getSearch();
            String sort = request.getSort() != null ? request.getSort().toLowerCase() : "newest";

            LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                    ? LocalDateTime.parse(request.getStartDate()) : null;
            LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                    ? LocalDateTime.parse(request.getEndDate()) : null;

            boolean isStatSort = List.of(
                    "total_orders_high", "total_orders_low",
                    "success_rate_high", "success_rate_low",
                    "return_rate_high", "return_rate_low"
            ).contains(sort);

            Specification<Address> spec = Specification
                    .where(RecipientAddressSpecification.userId(shopId))
                    .and(RecipientAddressSpecification.type(AddressType.RECIPIENT))
                    .and(RecipientAddressSpecification.keyword(keyword))
                    .and(RecipientAddressSpecification.createdAtBetween(startDate, endDate));

            List<RecipientAddressResponse> mappedList;

            if (isStatSort) {
                List<Address> all = addressRepository.findAll(spec);

                mappedList = all.stream()
                        .map(address -> buildResponse(shopId, address))
                        .sorted(statComparator(sort))
                        .toList();

                int total = mappedList.size();
                int fromIndex = Math.min((page - 1) * limit, total);
                int toIndex = Math.min(fromIndex + limit, total);
                List<RecipientAddressResponse> paged = mappedList.subList(fromIndex, toIndex);

                int totalPages = (int) Math.ceil((double) total / limit);
                Pagination pagination = new Pagination(total, page, limit, totalPages);

                ListResponse<RecipientAddressResponse> data = new ListResponse<>();
                data.setList(paged);
                data.setPagination(pagination);

                return new ApiResponse<>(true, "Lấy danh sách thành công", data);

            } else {
                Sort sortOpt = switch (sort) {
                    case "oldest" -> Sort.by("createdAt").ascending();
                    default -> Sort.by("createdAt").descending(); // newest
                };

                Pageable pageable = PageRequest.of(page - 1, limit, sortOpt);
                Page<Address> pageData = addressRepository.findAll(spec, pageable);

                mappedList = pageData.getContent()
                        .stream()
                        .map(address -> buildResponse(shopId, address))
                        .toList();

                int total = (int) pageData.getTotalElements();
                Pagination pagination = new Pagination(total, page, limit, pageData.getTotalPages());

                ListResponse<RecipientAddressResponse> data = new ListResponse<>();
                data.setList(mappedList);
                data.setPagination(pagination);

                return new ApiResponse<>(true, "Lấy danh sách thành công", data);
            }

        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<RecipientAddressResponse> create(int userId, RecipientAddressUserRequest request) {
        try {
            validateForm(request);

            Integer shopId = userService.getShopId(userId);

            User user = userService.getUser(shopId);

            String fullAddress = AddressUtils.buildFullAddress(
                    request.getDetail(),
                    request.getWardName(),
                    request.getCityName());

            boolean exists = addressRepository.existsByUserIdAndPhoneNumberAndFullAddressAndType(
                    shopId, request.getPhoneNumber(), fullAddress, AddressType.RECIPIENT);
            if (exists) {
                return new ApiResponse<>(false, "Địa chỉ này đã tồn tại", null);
            }

            Address address = new Address();
            address.setUser(user);
            address.setCityCode(request.getCityCode());
            address.setWardCode(request.getWardCode());
            address.setDetail(request.getDetail());
            address.setName(request.getName());
            address.setPhoneNumber(request.getPhoneNumber());
            address.setType(AddressType.RECIPIENT);
            address.setFullAddress(fullAddress);
            address.setCityName(request.getCityName());
            address.setWardName(request.getWardName());
            address.setLatitude(request.getLatitude());
            address.setLongitude(request.getLongitude());

            addressRepository.save(address);

            LocalDateTime latestOrderDate = orderRepository
                    .findFirstByUserIdAndRecipientPhoneOrderByCreatedAtDesc(shopId, request.getPhoneNumber())
                    .map(Order::getCreatedAt)
                    .orElse(null);

            RecipientStats stats = buildStatsForShop(
                    shopId,
                    request.getPhoneNumber(),
                    fullAddress,
                    new RecipientAddressResult(null, latestOrderDate, RecipientAddressType.NONE));

            return new ApiResponse<>(true, "Thêm địa chỉ thành công", RecipientAddressResponse.builder()
                    .address(buildAddressFromSaved(address))
                    .recipientStats(stats)
                    .build());
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<RecipientAddressResponse> update(int userId, int id, RecipientAddressUserRequest request) {
        try {
            validateForm(request);

            Integer shopId = userService.getShopId(userId);

            Address address = addressRepository.findByIdAndUserIdAndType(id, shopId, AddressType.RECIPIENT)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

            String fullAddress = AddressUtils.buildFullAddress(
                    request.getDetail(),
                    request.getWardName(),
                    request.getCityName());

            boolean exists = addressRepository.existsByUserIdAndPhoneNumberAndFullAddressAndTypeAndIdNot(
                    shopId, request.getPhoneNumber(), fullAddress, AddressType.RECIPIENT, id);
            if (exists) {
                return new ApiResponse<>(false, "Địa chỉ này đã tồn tại", null);
            }

            address.setCityCode(request.getCityCode());
            address.setWardCode(request.getWardCode());
            address.setCityName(request.getCityName());
            address.setWardName(request.getWardName());
            address.setFullAddress(fullAddress);
            address.setLongitude(request.getLongitude());
            address.setLatitude(request.getLatitude());
            address.setDetail(request.getDetail());
            address.setName(request.getName());
            address.setPhoneNumber(request.getPhoneNumber());

            addressRepository.save(address);

            LocalDateTime latestOrderDate = orderRepository
                    .findFirstByUserIdAndRecipientPhoneOrderByCreatedAtDesc(shopId, request.getPhoneNumber())
                    .map(Order::getCreatedAt)
                    .orElse(null);

            RecipientStats stats = buildStatsForShop(
                    shopId,
                    request.getPhoneNumber(),
                    fullAddress,
                    new RecipientAddressResult(null, latestOrderDate, RecipientAddressType.NONE));

            return new ApiResponse<>(true, "Cập nhật địa chỉ thành công", RecipientAddressResponse.builder()
                    .address(buildAddressFromSaved(address))
                    .recipientStats(stats)
                    .build());
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    public ApiResponse<Boolean> delete(int userId, int id) {
        try {
            Integer shopId = userService.getShopId(userId);

            Address address = addressRepository.findByIdAndUserIdAndType(id, shopId, AddressType.RECIPIENT)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

            if (Boolean.TRUE.equals(address.getIsDefault())) {
                return new ApiResponse<>(false, "Vui lòng chọn địa chỉ mặc định khác trước khi xóa", false);
            }

            addressRepository.delete(address);
            return new ApiResponse<>(true, "Xóa địa chỉ thành công", true);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), false);
        }
    }

    public ApiResponse<RecipientSuggestionAddressResponse> getRecipientSuggestion(
            Integer userId,
            RecipientSuggestionRequest request
    ) {
        try {
            String phone = request.getPhone();

            Integer shopId = userService.getShopId(userId);

            List<RecipientAddressSuggestionItem> items = resolveAddressSuggestions(shopId, phone);
            RecipientAddressType type = items.isEmpty() ? RecipientAddressType.NONE
                    : items.get(0).type();

            RecipientSuggestionAddressResponse data = RecipientSuggestionAddressResponse.builder()
                    .addresses(items.stream()
                            .map(i -> RecipientAddressWithStats.builder()
                                    .address(i.address())
                                    .recipientStats(i.stats())
                                    .build())
                            .toList())
                    .type(type)
                    .build();

            return new ApiResponse<>(true, "Lấy địa chỉ gợi ý thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public byte[] export(Integer userId, UserRecipientAddressSearchRequest request) {
        Integer shopId = userService.getShopId(userId);

        String keyword = request.getSearch();
        String sort = request.getSort() != null ? request.getSort().toLowerCase() : "newest";

        LocalDateTime startDate = request.getStartDate() != null && !request.getStartDate().isBlank()
                ? LocalDateTime.parse(request.getStartDate()) : null;
        LocalDateTime endDate = request.getEndDate() != null && !request.getEndDate().isBlank()
                ? LocalDateTime.parse(request.getEndDate()) : null;

        boolean isStatSort = List.of(
                "total_orders_high", "total_orders_low",
                "success_rate_high", "success_rate_low",
                "return_rate_high", "return_rate_low"
        ).contains(sort);

        Specification<Address> spec = Specification
                .where(RecipientAddressSpecification.userId(shopId))
                .and(RecipientAddressSpecification.type(AddressType.RECIPIENT))
                .and(RecipientAddressSpecification.keyword(keyword))
                .and(RecipientAddressSpecification.createdAtBetween(startDate, endDate));

        List<RecipientAddressResponse> list;

        if (isStatSort) {
            list = addressRepository.findAll(spec)
                    .stream()
                    .map(address -> buildResponse(shopId, address))
                    .sorted(statComparator(sort))
                    .toList();
        } else {
            Sort sortOpt = "oldest".equals(sort)
                    ? Sort.by("createdAt").ascending()
                    : Sort.by("createdAt").descending();

            list = addressRepository.findAll(spec, sortOpt)
                    .stream()
                    .map(address -> buildResponse(shopId, address))
                    .toList();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("RecipientAddresses");

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
                    "Tên",
                    "Số điện thoại",
                    "Địa chỉ",
                    "Tổng đơn",
                    "Tỉ lệ thành công",
                    "Tỉ lệ hoàn hàng"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (RecipientAddressResponse r : list) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(r.getAddress().getName() != null ? r.getAddress().getName() : "");
                row.createCell(1).setCellValue(r.getAddress().getPhoneNumber() != null ? r.getAddress().getPhoneNumber() : "");
                row.createCell(2).setCellValue(r.getAddress().getFullAddress() != null ? r.getAddress().getFullAddress() : "");
                row.createCell(3).setCellValue(r.getRecipientStats().getTotalSystemOrders());
                row.createCell(4).setCellValue(r.getRecipientStats().getSuccessRate() + "%");
                row.createCell(5).setCellValue(r.getRecipientStats().getReturnedRate() + "%");
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

    private List<RecipientAddressSuggestionItem> resolveAddressSuggestions(Integer userId, String phone) {
        List<Address> saved = addressRepository
                .findByUserIdAndPhoneNumberAndType(userId, phone, AddressType.RECIPIENT);

        if (!saved.isEmpty()) {
            return saved.stream()
                    .map(addr -> {
                        LocalDateTime latestOrderDate = orderRepository
                                .findFirstByUserIdAndRecipientPhoneAndRecipientFullAddressOrderByCreatedAtDesc(
                                        userId, addr.getPhoneNumber(), addr.getFullAddress())
                                .map(Order::getCreatedAt)
                                .orElse(null);

                        // Suggestion: dùng thống kê toàn hệ thống (không có userId)
                        RecipientStats stats = buildStats(
                                addr.getPhoneNumber(),
                                addr.getFullAddress(),
                                new RecipientAddressResult(null, latestOrderDate, RecipientAddressType.NONE));

                        return new RecipientAddressSuggestionItem(
                                buildAddressFromSaved(addr), stats, RecipientAddressType.SAVED);
                    })
                    .sorted(Comparator.comparingLong(
                            (RecipientAddressSuggestionItem i) -> i.stats().getTotalSystemOrders()
                    ).reversed())
                    .toList();
        }

        return orderRepository
                .findMostUsedFullAddressByUserIdAndRecipientPhone(userId, phone)
                .flatMap(fullAddr -> orderRepository
                        .findFirstByUserIdAndRecipientPhoneAndRecipientFullAddressOrderByCreatedAtDesc(
                                userId, phone, fullAddr))
                .map(order -> {
                    RecipientStats stats = buildStats(
                            order.getRecipientPhone(),
                            order.getRecipientFullAddress(),
                            new RecipientAddressResult(null, order.getCreatedAt(), RecipientAddressType.NONE));

                    return List.of(new RecipientAddressSuggestionItem(
                            buildAddressFromOrder(order), stats, RecipientAddressType.HISTORY));
                })
                .orElse(List.of());
    }

    private RecipientStats buildStats(
            String phone,
            String fullAddress,
            RecipientAddressResult addressResult
    ) {
        long total = orderRepository.countByRecipientPhoneAndRecipientFullAddress(phone, fullAddress);
        long success = orderRepository.countByRecipientPhoneAndRecipientFullAddressAndStatusIn(
                phone, fullAddress, List.of(OrderStatus.DELIVERED, OrderStatus.PARTIAL_DELIVERY));
        long returned = orderRepository.countByRecipientPhoneAndRecipientFullAddressAndStatusIn(
                phone, fullAddress, List.of(OrderStatus.RETURNED, OrderStatus.PARTIAL_RETURN));

        return RecipientStats.builder()
                .totalSystemOrders(total)
                .successRate(calculateRate(total, success))
                .returnedRate(calculateRate(total, returned))
                .latestOrderDate(addressResult.latestOrderDate())
                .build();
    }

    private RecipientStats buildStatsForShop(
            int userId,
            String phone,
            String fullAddress,
            RecipientAddressResult addressResult
    ) {
        long total = orderRepository.countByUserIdAndRecipientPhoneAndRecipientFullAddress(
                userId, phone, fullAddress);
        long success = orderRepository.countByUserIdAndRecipientPhoneAndRecipientFullAddressAndStatusIn(
                userId, phone, fullAddress, List.of(OrderStatus.DELIVERED, OrderStatus.PARTIAL_DELIVERY));
        long returned = orderRepository.countByUserIdAndRecipientPhoneAndRecipientFullAddressAndStatusIn(
                userId, phone, fullAddress, List.of(OrderStatus.RETURNED, OrderStatus.PARTIAL_RETURN));

        return RecipientStats.builder()
                .totalSystemOrders(total)
                .successRate(calculateRate(total, success))
                .returnedRate(calculateRate(total, returned))
                .latestOrderDate(addressResult.latestOrderDate())
                .build();
    }

    private RecipientAddressResponse buildResponse(int userId, Address address) {
        LocalDateTime latestOrderDate = orderRepository
                .findFirstByUserIdAndRecipientPhoneAndRecipientFullAddressOrderByCreatedAtDesc(
                        userId, address.getPhoneNumber(), address.getFullAddress())
                .map(Order::getCreatedAt)
                .orElse(null);

        RecipientStats stats = buildStatsForShop(
                userId,
                address.getPhoneNumber(),
                address.getFullAddress(),
                new RecipientAddressResult(null, latestOrderDate, RecipientAddressType.NONE));

        return RecipientAddressResponse.builder()
                .address(buildAddressFromSaved(address))
                .recipientStats(stats)
                .build();
    }

    private RecipientAddress buildAddressFromSaved(Address addr) {
        return AddressMapper.toRecipientAddress(addr);
    }

    private RecipientAddress buildAddressFromOrder(Order o) {
        return RecipientAddress.builder()
                .name(o.getRecipientName())
                .phoneNumber(o.getRecipientPhone())
                .fullAddress(o.getRecipientFullAddress())
                .detail(o.getRecipientDetail())
                .cityCode(o.getRecipientCityCode())
                .cityName(o.getRecipientCityName())
                .wardCode(o.getRecipientWardCode())
                .wardName(o.getRecipientWardName())
                .latitude(o.getRecipientLatitude())
                .longitude(o.getRecipientLongitude())
                .build();
    }

    private double calculateRate(long total, long count) {
        if (total == 0) return 0;
        return Math.round((double) count / total * 1000) / 10.0;
    }

    private Comparator<RecipientAddressResponse> statComparator(String sort) {
        return switch (sort) {
            case "total_orders_high" -> Comparator.comparingLong(
                    (RecipientAddressResponse r) -> r.getRecipientStats().getTotalSystemOrders()
            ).reversed();
            case "total_orders_low" -> Comparator.comparingLong(
                    r -> r.getRecipientStats().getTotalSystemOrders()
            );
            case "success_rate_high" -> Comparator.comparingDouble(
                    (RecipientAddressResponse r) -> r.getRecipientStats().getSuccessRate()
            ).reversed();
            case "success_rate_low" -> Comparator.comparingDouble(
                    r -> r.getRecipientStats().getSuccessRate()
            );
            case "return_rate_high" -> Comparator.comparingDouble(
                    (RecipientAddressResponse r) -> r.getRecipientStats().getReturnedRate()
            ).reversed();
            case "return_rate_low" -> Comparator.comparingDouble(
                    r -> r.getRecipientStats().getReturnedRate()
            );
            default -> Comparator.comparing(
                    r -> r.getRecipientStats().getTotalSystemOrders()
            );
        };
    }

    private void validateForm(RecipientAddressUserRequest request) {
        StringBuilder missing = new StringBuilder();
        if (request.getDetail() == null || request.getDetail().isBlank())
            missing.append("Chi tiết địa chỉ, ");
        if (request.getCityCode() <= 0)
            missing.append("Mã thành phố, ");
        if (request.getWardCode() <= 0)
            missing.append("Mã phường/xã, ");
        if (request.getLatitude() <= 0)
            missing.append("Vĩ độ ");
        if (request.getLongitude() <= 0)
            missing.append("Kinh độ, ");
        if (request.getCityName() == null || request.getCityName().isBlank())
            missing.append("Tên thành phố, ");
        if (request.getWardName() == null || request.getWardName().isBlank())
            missing.append("Tên phường/xã, ");
        if (request.getName() == null || request.getName().isBlank())
            missing.append("Tên, ");
        if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())
            missing.append("Số điện thoại, ");

        if (missing.length() > 0) {
            throw new RuntimeException("Thiếu hoặc không hợp lệ: " + missing.substring(0, missing.length() - 2));
        }
    }

    private record RecipientAddressResult(
            RecipientAddress address,
            LocalDateTime latestOrderDate,
            RecipientAddressType type
    ) {}

    private record RecipientAddressSuggestionItem(
            RecipientAddress address,
            RecipientStats stats,
            RecipientAddressType type
    ) {}
}