package com.logistics.service.common.impl;

import com.logistics.dto.ServiceTypeDto;
import com.logistics.dto.ServiceTypeWithRateDto;
import com.logistics.entity.ServiceType;
import com.logistics.enums.ServiceTypeStatus;
import com.logistics.mapper.ServiceTypeMapper;
import com.logistics.repository.ServiceTypeRepository;
import com.logistics.service.common.ServiceTypePublicService;
import com.logistics.specification.ServiceTypeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceTypePublicServiceImpl implements ServiceTypePublicService {

    private final ServiceTypeRepository serviceTypeRepository;

    @Override
    public List<ServiceTypeDto> getServicesByStatus(ServiceTypeStatus status) {
        Specification<ServiceType> spec = (status == ServiceTypeStatus.ACTIVE)
                ? ServiceTypeSpecification.statusActive()
                : null;

        return serviceTypeRepository.findAll(spec)
                .stream()
                .map(ServiceTypeMapper::toDto)
                .toList();
    }

    @Override
    public List<ServiceTypeWithRateDto> getActiveServicesWithRates() {
        return serviceTypeRepository.findAllWithRatesByStatus(ServiceTypeStatus.ACTIVE)
                .stream()
                .map(ServiceTypeMapper::toDtoWithRate)
                .toList();
    }
}