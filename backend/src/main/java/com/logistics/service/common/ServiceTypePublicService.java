package com.logistics.service.common;

import com.logistics.dto.ServiceTypeDto;
import com.logistics.dto.ServiceTypeWithRateDto;
import com.logistics.enums.ServiceTypeStatus;

import java.util.List;

public interface ServiceTypePublicService {
    List<ServiceTypeDto> getServicesByStatus(ServiceTypeStatus status);
    List<ServiceTypeWithRateDto> getActiveServicesWithRates();
}