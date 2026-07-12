package com.logistics.service.manager.impl;

import com.logistics.dto.manager.attempt.AttemptHistoryListDto;
import com.logistics.entity.*;
import com.logistics.enums.DeliveryAttemptType;
import com.logistics.repository.DeliveryAttemptRepository;
import com.logistics.repository.PickupAttemptRepository;
import com.logistics.request.manager.attempt.AttemptSearchRequest;
import com.logistics.response.ListResponse;
import com.logistics.response.Pagination;
import com.logistics.service.manager.AttemptHistoryManagerService;
import com.logistics.service.manager.EmployeeManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttemptHistoryManagerServiceImpl implements AttemptHistoryManagerService {

    private static final String CATEGORY_PICKUP = "PICKUP";
    private static final String CATEGORY_DELIVERY = "DELIVERY";
    private static final String CATEGORY_RETURN_DELIVERY = "RETURN_DELIVERY";

    private final PickupAttemptRepository pickupAttemptRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final EmployeeManagerService employeeManagerService;

    @Override
    public ListResponse<AttemptHistoryListDto> list(int userId, AttemptSearchRequest request) {
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int limit = request.getLimit() == null || request.getLimit() < 1 ? 10 : request.getLimit();

        LocalDateTime from = parseDate(request.getStartDate(), true);
        LocalDateTime to = parseDate(request.getEndDate(), false);

        String category = request.getCategory() == null ? "" : request.getCategory().toUpperCase(Locale.ROOT);
        String status = request.getStatus() == null ? "" : request.getStatus().toUpperCase(Locale.ROOT);
        String sort = request.getSort() == null ? "NEWEST" : request.getSort().toUpperCase(Locale.ROOT);
        String search = request.getSearch() == null ? "" : request.getSearch().trim().toLowerCase(Locale.ROOT);

        Office userOffice = employeeManagerService.getManagedOfficeByUserId(userId);
        Integer officeId = userOffice.getId();

        List<AttemptHistoryListDto> merged = new ArrayList<>();

        boolean includePickup = category.isEmpty() || CATEGORY_PICKUP.equals(category);
        boolean includeDelivery = category.isEmpty() || CATEGORY_DELIVERY.equals(category);
        boolean includeReturn = category.isEmpty() || CATEGORY_RETURN_DELIVERY.equals(category);

        if (includePickup) {
            for (PickupAttempt pa : pickupAttemptRepository.findByOfficeAndDateRange(officeId, from, to)) {
                AttemptHistoryListDto dto = toDto(pa);
                if (matchesStatus(dto, status, true) && matchesSearch(dto, search)) {
                    merged.add(dto);
                }
            }
        }

        if (includeDelivery || includeReturn) {
            for (DeliveryAttempt da : deliveryAttemptRepository.findByOfficeAndDateRange(officeId, from, to)) {
                AttemptHistoryListDto dto = toDto(da);
                String resolvedCategory = da.getAttemptType() == DeliveryAttemptType.RETURN_DELIVERY
                        ? CATEGORY_RETURN_DELIVERY
                        : CATEGORY_DELIVERY;
                if (CATEGORY_RETURN_DELIVERY.equals(resolvedCategory) && !includeReturn) continue;
                if (CATEGORY_DELIVERY.equals(resolvedCategory) && !includeDelivery) continue;
                if (matchesStatus(dto, status, false) && matchesSearch(dto, search)) {
                    merged.add(dto);
                }
            }
        }

        Comparator<AttemptHistoryListDto> comparator = "OLDEST".equals(sort)
                ? Comparator.comparing(AttemptHistoryListDto::getAttemptedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(AttemptHistoryListDto::getAttemptedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()));
        merged.sort(comparator);

        int total = merged.size();
        int totalPages = (int) Math.ceil((double) total / limit);
        int fromIndex = Math.min((page - 1) * limit, total);
        int toIndex = Math.min(fromIndex + limit, total);
        List<AttemptHistoryListDto> pageContent = merged.subList(fromIndex, toIndex);

        Pagination pagination = new Pagination(total, page, limit, totalPages);
        ListResponse<AttemptHistoryListDto> response = new ListResponse<>();
        response.setList(pageContent);
        response.setPagination(pagination);
        return response;
    }

    private AttemptHistoryListDto toDto(PickupAttempt pa) {
        AttemptHistoryListDto dto = new AttemptHistoryListDto();
        dto.setId(pa.getId());
        dto.setAttemptCategory(CATEGORY_PICKUP);
        dto.setAttemptType(null);
        Order order = pa.getOrder();
        User shipper = pa.getShipper();
        dto.setOrderId(order == null ? null : order.getId());
        dto.setTrackingNumber(order == null ? null : order.getTrackingNumber());
        dto.setAttemptNumber(pa.getAttemptNumber());
        dto.setPickupStatus(pa.getStatus());
        dto.setDeliveryStatus(null);
        dto.setFailReason(pa.getFailReason() == null ? null : pa.getFailReason().name());
        dto.setNote(pa.getNote());
        dto.setProofImageUrl(pa.getProofImageUrl());
        dto.setAttemptedAt(pa.getAttemptedAt());
        dto.setShipperId(shipper == null ? null : shipper.getId());
        dto.setShipperName(shipper == null ? null : shipper.getFullName());
        dto.setShipperPhone(shipper == null ? null : shipper.getPhoneNumber());
        return dto;
    }

    private AttemptHistoryListDto toDto(DeliveryAttempt da) {
        AttemptHistoryListDto dto = new AttemptHistoryListDto();
        dto.setId(da.getId());
        String category = da.getAttemptType() == DeliveryAttemptType.RETURN_DELIVERY
                ? CATEGORY_RETURN_DELIVERY
                : CATEGORY_DELIVERY;
        dto.setAttemptCategory(category);
        dto.setAttemptType(da.getAttemptType());
        Order order = da.getOrder();
        User shipper = da.getShipper();
        dto.setOrderId(order == null ? null : order.getId());
        dto.setTrackingNumber(order == null ? null : order.getTrackingNumber());
        dto.setAttemptNumber(da.getAttemptNumber());
        dto.setPickupStatus(null);
        dto.setDeliveryStatus(da.getStatus());
        dto.setFailReason(da.getFailReason() == null ? null : da.getFailReason().name());
        dto.setNote(da.getNote());
        dto.setProofImageUrl(da.getProofImageUrl());
        dto.setAttemptedAt(da.getAttemptedAt());
        dto.setShipperId(shipper == null ? null : shipper.getId());
        dto.setShipperName(shipper == null ? null : shipper.getFullName());
        dto.setShipperPhone(shipper == null ? null : shipper.getPhoneNumber());
        return dto;
    }

    private boolean matchesStatus(AttemptHistoryListDto dto, String status, boolean isPickup) {
        if (status.isEmpty() || "ALL".equals(status)) return true;
        if (isPickup) {
            return dto.getPickupStatus() != null && dto.getPickupStatus().name().equals(status);
        }
        return dto.getDeliveryStatus() != null && dto.getDeliveryStatus().name().equals(status);
    }

    private boolean matchesSearch(AttemptHistoryListDto dto, String search) {
        if (search.isEmpty()) return true;
        if (dto.getTrackingNumber() != null && dto.getTrackingNumber().toLowerCase(Locale.ROOT).contains(search)) {
            return true;
        }
        if (dto.getShipperName() != null && dto.getShipperName().toLowerCase(Locale.ROOT).contains(search)) {
            return true;
        }
        if (dto.getShipperPhone() != null && dto.getShipperPhone().toLowerCase(Locale.ROOT).contains(search)) {
            return true;
        }
        return false;
    }

    private LocalDateTime parseDate(String raw, boolean startOfDay) {
        if (raw == null || raw.isBlank()) return null;
        try {
            LocalDateTime dt = LocalDateTime.parse(raw);
            return startOfDay ? dt.toLocalDate().atStartOfDay()
                    : dt.toLocalDate().atTime(23, 59, 59);
        } catch (Exception ignore) {
            // try ISO date-only
        }
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(raw);
            return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
        } catch (Exception ignore) {
            return null;
        }
    }
}
