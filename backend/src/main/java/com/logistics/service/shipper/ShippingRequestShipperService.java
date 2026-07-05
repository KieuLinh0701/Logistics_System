package com.logistics.service.shipper;

import com.logistics.entity.ShippingRequest;

import java.util.List;

public interface ShippingRequestShipperService {

    List<ShippingRequest> listForCurrentShipper();

    void accept(Integer requestId);
}
