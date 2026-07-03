package com.logistics.service.user;

import com.logistics.dto.user.shippingRequest.UserShippingRequestDetailDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestEditDto;
import com.logistics.dto.user.shippingRequest.UserShippingRequestListDto;
import com.logistics.entity.Order;
import com.logistics.enums.ShippingRequestType;
import com.logistics.request.user.shippingRequest.UserShippingRequestForm;
import com.logistics.request.user.shippingRequest.UserShippingRequestSearchRequest;
import com.logistics.response.ListResponse;

public interface ShippingRequestUserService {
    ListResponse<UserShippingRequestListDto> list(int userId, UserShippingRequestSearchRequest request);
    UserShippingRequestDetailDto getShippingRequestById(int userId, int id);
    UserShippingRequestEditDto getShippingRequestByIdForEdit(int userId, int id);
    void create(int userId, UserShippingRequestForm request);
    void update(int userId, int id, UserShippingRequestForm request);
    void cancel(int userId, int id);
    byte[] export(Integer userId, UserShippingRequestSearchRequest request);
    boolean hasActiveRequest(
            Integer userId,
            ShippingRequestType type,
            String requestContent,
            Order order);
}