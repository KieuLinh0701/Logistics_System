package com.logistics.service.manager;

import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestDetailDto;
import com.logistics.dto.manager.shippingRequest.ManagerShippingRequestListDto;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestForm;
import com.logistics.request.manager.shippingRequest.ManagerShippingRequestSearchRequest;
import com.logistics.response.ListResponse;

public interface ShippingRequestManagerService {
    ListResponse<ManagerShippingRequestListDto> list(int userId, ManagerShippingRequestSearchRequest request);
    byte[] export(int userId, ManagerShippingRequestSearchRequest request);
    ManagerShippingRequestDetailDto getShippingRequestById(int userId, int id);
    void processing(int userId, int id, ManagerShippingRequestForm request);
}