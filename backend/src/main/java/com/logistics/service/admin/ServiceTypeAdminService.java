package com.logistics.service.admin;

import com.logistics.request.admin.CreateServiceTypeRequest;
import com.logistics.request.admin.UpdateServiceTypeRequest;

import java.util.Map;

public interface ServiceTypeAdminService {

    Map<String, Object> listServiceTypes(int page, int limit, String search);

    Map<String, Object> getServiceTypeById(Integer id);

    void createServiceType(CreateServiceTypeRequest request);

    void updateServiceType(Integer id, UpdateServiceTypeRequest request);

    void deleteServiceType(Integer id);
}
