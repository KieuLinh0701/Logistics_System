package com.logistics.service.admin;

import com.logistics.entity.ShippingRequest;
import com.logistics.enums.ShippingRequestStatus;

import java.util.List;
import java.util.Map;

public interface ShippingRequestAdminService {

    List<Map<String, Object>> listAll();

    ShippingRequest detail(Integer id);

    void assignOffice(Integer requestId, Integer officeId);

    void updateStatus(Integer requestId, ShippingRequestStatus status);
}
