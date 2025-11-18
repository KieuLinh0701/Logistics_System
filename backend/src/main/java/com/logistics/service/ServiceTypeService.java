package com.logistics.service;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.logistics.dto.ServiceTypeDto;
import com.logistics.dto.ServiceTypeWithRateDto;
import com.logistics.entity.ServiceType;
import com.logistics.enums.ServiceTypeStatus;
import com.logistics.mapper.ServiceTypeMapper;
import com.logistics.repository.ServiceTypeRepository;
import com.logistics.response.ApiResponse;
import com.logistics.specification.ServiceTypeSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    public ApiResponse<List<ServiceTypeDto>> getServicesByStatus(ServiceTypeStatus status) {
        try {
            Specification<ServiceType> spec = (status == ServiceTypeStatus.ACTIVE)
                    ? ServiceTypeSpecification.statusActive()
                    : null;

            List<ServiceTypeDto> services = serviceTypeRepository.findAll(spec)
                    .stream()
                    .map(ServiceTypeMapper::toDto)
                    .toList();

            return new ApiResponse<>(true, "Lấy danh sách dịch vụ thành công", services);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách dịch vụ: " + e.getMessage(), null);
        }
    }

    public ApiResponse<List<ServiceTypeWithRateDto>> getActiveServicesWithRates() {
        try {
            List<ServiceTypeWithRateDto> list = serviceTypeRepository.findAllWithRatesByStatus(ServiceTypeStatus.ACTIVE)
                    .stream()
                    .map(ServiceTypeMapper::toDtoWithRate)
                    .toList();
            return new ApiResponse<>(true, "Lấy dịch vụ kèm giá thành công", list);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy dịch vụ kèm giá: " + e.getMessage(), null);
        }
    }
}