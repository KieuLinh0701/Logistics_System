package com.logistics.service.user;

import com.logistics.dto.AddressDto;
import com.logistics.entity.Address;
import com.logistics.entity.Order;
import com.logistics.entity.User;
import com.logistics.enums.AddressType;
import com.logistics.enums.OrderStatus;
import com.logistics.enums.RecipientAddressType;
import com.logistics.mapper.AddressMapper;
import com.logistics.repository.AddressRepository;
import com.logistics.repository.OrderRepository;
import com.logistics.repository.UserRepository;
import com.logistics.request.user.address.AddressUserRequest;
import com.logistics.request.user.recipientaddress.RecipientAddressUserRequest;
import com.logistics.request.user.recipientaddress.RecipientSuggestionRequest;
import com.logistics.request.user.recipientaddress.UserRecipientAddressSearchRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.response.user.recipientaddress.RecipientAddress;
import com.logistics.response.user.recipientaddress.RecipientAddressResponse;
import com.logistics.response.user.recipientaddress.RecipientStats;
import com.logistics.response.user.recipientaddress.RecipientSuggestionAddressResponse;
import com.logistics.specification.RecipientAddressSpecification;
import com.logistics.utils.AddressUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecipientAddressUserService {

    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public ApiResponse<ListResponse<RecipientAddressResponse>> list(int userId, UserRecipientAddressSearchRequest request) {
        try {
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
                    .where(RecipientAddressSpecification.userId(userId))
                    .and(RecipientAddressSpecification.type(AddressType.RECIPIENT))
                    .and(RecipientAddressSpecification.keyword(keyword))
                    .and(RecipientAddressSpecification.createdAtBetween(startDate, endDate));

            List<RecipientAddressResponse> mappedList;

            if (isStatSort) {
                List<Address> all = addressRepository.findAll(spec);

                mappedList = all.stream()
                        .map(address -> buildResponse(userId, address))
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
                        .map(address -> buildResponse(userId, address))
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

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            String fullAddress = AddressUtils.buildFullAddress(
                    request.getDetail(),
                    request.getWardName(),
                    request.getCityName());

            boolean exists = addressRepository.existsByUserIdAndPhoneNumberAndFullAddressAndType(
                    userId, request.getPhoneNumber(), fullAddress, AddressType.RECIPIENT);
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
                    .findFirstByUserIdAndRecipientPhoneOrderByCreatedAtDesc(userId, request.getPhoneNumber())
                    .map(Order::getCreatedAt)
                    .orElse(null);

            RecipientStats stats = buildStats(
                    request.getPhoneNumber(),
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

            Address address = addressRepository.findByIdAndUserIdAndType(id, userId, AddressType.RECIPIENT)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

            String fullAddress = AddressUtils.buildFullAddress(
                    request.getDetail(),
                    request.getWardName(),
                    request.getCityName());

            boolean exists = addressRepository.existsByUserIdAndPhoneNumberAndFullAddressAndTypeAndIdNot(
                    userId, request.getPhoneNumber(), fullAddress, AddressType.RECIPIENT, id);
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
                    .findFirstByUserIdAndRecipientPhoneOrderByCreatedAtDesc(userId, request.getPhoneNumber())
                    .map(Order::getCreatedAt)
                    .orElse(null);

            RecipientStats stats = buildStats(
                    request.getPhoneNumber(),
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
            Address address = addressRepository.findByIdAndUserIdAndType(id, userId, AddressType.RECIPIENT)
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

            RecipientAddressResult addressResult = resolveAddress(userId, phone);
            RecipientStats stats = buildStats(phone, addressResult);

            RecipientSuggestionAddressResponse data = RecipientSuggestionAddressResponse.builder()
                    .address(addressResult.address())
                    .recipientStats(stats)
                    .type(addressResult.type())
                    .build();

            return new ApiResponse<>(true, "Lấy địa chỉ gợi ý thành công", data);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    private RecipientAddressResult resolveAddress(Integer userId, String phone) {
        return findSavedAddress(userId, phone)
                .orElseGet(() -> findAddressFromHistory(userId, phone));
    }

    private Optional<RecipientAddressResult> findSavedAddress(Integer userId, String phone) {
        return addressRepository
                .findFirstByUserIdAndPhoneNumberAndType(
                        userId, phone, AddressType.RECIPIENT)
                .map(addr -> new RecipientAddressResult(
                        buildAddressFromSaved(addr),
                        null,
                        RecipientAddressType.SAVED));
    }

    private RecipientAddressResult findAddressFromHistory(Integer userId, String phone) {
        return orderRepository
                .findFirstByUserIdAndRecipientPhoneOrderByCreatedAtDesc(userId, phone)
                .map(order -> new RecipientAddressResult(
                        buildAddressFromOrder(order),
                        order.getCreatedAt(),
                        RecipientAddressType.HISTORY))
                .orElse(new RecipientAddressResult(
                        RecipientAddress.builder()
                                .build(),
                        null,
                        RecipientAddressType.NONE));
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

    private RecipientStats buildStats(
            String phone,
            RecipientAddressResult addressResult
    ) {
        long total = orderRepository.countByRecipientPhone(phone);
        long success = countByStatuses(phone, OrderStatus.DELIVERED, OrderStatus.PARTIAL_DELIVERY);
        long returned = countByStatuses(phone, OrderStatus.RETURNED, OrderStatus.PARTIAL_RETURN);
        double successRate = calculateRate(total, success);
        double returnedRate = calculateRate(total, returned);

        return RecipientStats.builder()
                .totalSystemOrders(total)
                .successRate(successRate)
                .returnedRate(returnedRate)
                .latestOrderDate(addressResult.latestOrderDate())
                .build();
    }

    private long countByStatuses(String phone, OrderStatus... statuses) {
        return orderRepository.countByRecipientPhoneAndStatusIn(phone, List.of(statuses));
    }

    private double calculateRate(long total, long count) {
        if (total == 0) return 0;
        return Math.round((double) count / total * 1000) / 10.0;
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
            // Bỏ dấu phẩy cuối
            throw new RuntimeException("Thiếu hoặc không hợp lệ: " + missing.substring(0, missing.length() - 2));
        }
    }

    private RecipientAddressResponse buildResponse(int userId, Address address) {
        LocalDateTime latestOrderDate = orderRepository
                .findFirstByUserIdAndRecipientPhoneOrderByCreatedAtDesc(userId, address.getPhoneNumber())
                .map(Order::getCreatedAt)
                .orElse(null);

        RecipientStats stats = buildStats(
                address.getPhoneNumber(),
                new RecipientAddressResult(null, latestOrderDate, RecipientAddressType.NONE));

        return RecipientAddressResponse.builder()
                .address(buildAddressFromSaved(address))
                .recipientStats(stats)
                .build();
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

    private record RecipientAddressResult(
            RecipientAddress address,
            LocalDateTime latestOrderDate,
            RecipientAddressType type
    ) {
    }
}