package com.logistics.service.common;

import com.logistics.request.common.shippingRequest.PublicShippingRequestForm;

public interface ShippingRequestPublicService {
    void create(PublicShippingRequestForm request);
}