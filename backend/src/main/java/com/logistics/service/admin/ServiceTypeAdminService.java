package com.logistics.service.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.entity.ServiceType;
import com.logistics.enums.ServiceTypeStatus;
import com.logistics.repository.ServiceTypeRepository;
import com.logistics.request.admin.CreateServiceTypeRequest;
import com.logistics.request.admin.UpdateServiceTypeRequest;
import com.logistics.response.ApiResponse;
import com.logistics.response.Pagination;

@Service
public class ServiceTypeAdminService {

    @Autowired
    private ServiceTypeRepository serviceTypeRepository;

    public ApiResponse<Map<String, Object>> listServiceTypes(int page, int limit, String search) {
        try {
            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
            Page<ServiceType> serviceTypePage;

            if (search != null && !search.trim().isEmpty()) {
                serviceTypePage = serviceTypeRepository.findAll((root, query, cb) ->
                        cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"), pageable);
            } else {
                serviceTypePage = serviceTypeRepository.findAll(pageable);
            }

            List<Map<String, Object>> data = serviceTypePage.getContent().stream()
                    .map(this::mapServiceType)
                    .collect(Collectors.toList());

            Pagination pagination = new Pagination(
                    (int) serviceTypePage.getTotalElements(),
                    page,
                    limit,
                    serviceTypePage.getTotalPages());

            Map<String, Object> result = new HashMap<>();
            result.put("data", data);
            result.put("pagination", pagination);

            return new ApiResponse<>(true, "Lấy danh sách loại dịch vụ thành công", result);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi: " + e.getMessage(), null);
        }
    }

    public ApiResponse<Map<String, Object>> getServiceTypeById(Integer id) {
        try {
            ServiceType serviceType = serviceTypeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại dịch vụ"));
            return new ApiResponse<>(true, "Lấy thông tin loại dịch vụ thành công", mapServiceType(serviceType));
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createServiceType(CreateServiceTypeRequest request) {
        try {
            if (request.getName() == null || request.getName().isBlank()) {
                return new ApiResponse<>(false, "Tên loại dịch vụ không được để trống", null);
            }

            ServiceType serviceType = new ServiceType();
            serviceType.setName(request.getName());
            serviceType.setDescription(request.getDescription());
            serviceType.setStatus(resolveStatus(request.getStatus()));
            serviceType.setDeliveryTime(buildDeliveryTime(request));
            serviceType = serviceTypeRepository.save(serviceType);

            return new ApiResponse<>(true, "Tạo loại dịch vụ thành công", mapServiceType(serviceType));
        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(false, "Trạng thái không hợp lệ", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<Map<String, Object>> updateServiceType(Integer id, UpdateServiceTypeRequest request) {
        try {
            ServiceType serviceType = serviceTypeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại dịch vụ"));

            if (request.getName() != null) serviceType.setName(request.getName());
            if (request.getDescription() != null) serviceType.setDescription(request.getDescription());
            if (request.getStatus() != null) serviceType.setStatus(resolveStatus(request.getStatus()));

            String deliveryTime = buildDeliveryTime(request);
            if (deliveryTime != null) {
                serviceType.setDeliveryTime(deliveryTime);
            }

            serviceType = serviceTypeRepository.save(serviceType);
            return new ApiResponse<>(true, "Cập nhật loại dịch vụ thành công", mapServiceType(serviceType));
        } catch (IllegalArgumentException e) {
            return new ApiResponse<>(false, "Trạng thái không hợp lệ", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    @Transactional
    public ApiResponse<String> deleteServiceType(Integer id) {
        try {
            ServiceType serviceType = serviceTypeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại dịch vụ"));
            serviceTypeRepository.delete(serviceType);
            return new ApiResponse<>(true, "Xóa loại dịch vụ thành công", null);
        } catch (Exception e) {
            return new ApiResponse<>(false, e.getMessage(), null);
        }
    }

    private ServiceTypeStatus resolveStatus(String status) {
        if (status == null || status.isBlank()) {
            return ServiceTypeStatus.ACTIVE;
        }
        return ServiceTypeStatus.valueOf(status.toUpperCase());
    }

    private String buildDeliveryTime(CreateServiceTypeRequest request) {
        if (request.getDeliveryTimeFrom() != null && request.getDeliveryTimeTo() != null
                && request.getDeliveryTimeUnit() != null && !request.getDeliveryTimeUnit().isBlank()) {
            return String.format("%d - %d %s",
                    request.getDeliveryTimeFrom(),
                    request.getDeliveryTimeTo(),
                    request.getDeliveryTimeUnit());
        }
        return request.getDeliveryTime();
    }

    private String buildDeliveryTime(UpdateServiceTypeRequest request) {
        if (request.getDeliveryTimeFrom() != null && request.getDeliveryTimeTo() != null
                && request.getDeliveryTimeUnit() != null && !request.getDeliveryTimeUnit().isBlank()) {
            return String.format("%d - %d %s",
                    request.getDeliveryTimeFrom(),
                    request.getDeliveryTimeTo(),
                    request.getDeliveryTimeUnit());
        }
        return request.getDeliveryTime();
    }

    private Map<String, Object> mapServiceType(ServiceType serviceType) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", serviceType.getId());
        map.put("name", serviceType.getName());
        map.put("description", serviceType.getDescription());
        map.put("status", serviceType.getStatus() != null ? serviceType.getStatus().name() : null);
        map.put("deliveryTime", serviceType.getDeliveryTime());
        map.put("createdAt", serviceType.getCreatedAt());
        map.put("updatedAt", serviceType.getUpdatedAt());
        return map;
    }
}

