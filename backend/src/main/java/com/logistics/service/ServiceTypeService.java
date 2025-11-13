package com.logistics.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.logistics.dto.serviceType.ServiceTypeDto;
import com.logistics.entity.ServiceType;
import com.logistics.enums.ServiceType.ServiceTypeStatus;
import com.logistics.mapper.ServiceTypeMapper;
import com.logistics.repository.ServiceTypeRepository;
import com.logistics.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServiceTypeService {

        private final ServiceTypeRepository serviceTypeRepository;

        public ApiResponse<List<ServiceTypeDto>> getServicesByStatus(ServiceTypeStatus status) {
                List<ServiceTypeDto> services = serviceTypeRepository.findByStatus(status)
                                .stream()
                                .map(ServiceTypeMapper::toDto)
                                .toList();
                return new ApiResponse<>(true, "Lấy danh sách dịch vụ đang hoạt động thành công", services);
        }
}