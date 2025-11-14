package com.logistics.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.logistics.dto.ServiceTypeDto;
import com.logistics.enums.ServiceTypeStatus;
import com.logistics.mapper.ServiceTypeMapper;
import com.logistics.repository.ServiceTypeRepository;
import com.logistics.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    public ApiResponse<List<ServiceTypeDto>> getServicesByStatus(ServiceTypeStatus status) {
        try {
            List<ServiceTypeDto> services = serviceTypeRepository.findByStatus(status)
                    .stream()
                    .map(ServiceTypeMapper::toDto)
                    .toList();
            return new ApiResponse<>(true, "Lấy danh sách dịch vụ thành công", services);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Lỗi khi lấy danh sách dịch vụ: " + e.getMessage(), null);
        }
    }
}