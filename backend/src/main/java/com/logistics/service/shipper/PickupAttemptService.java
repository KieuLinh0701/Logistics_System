package com.logistics.service.shipper;

import com.logistics.enums.PickupAttemptStatus;
import com.logistics.enums.PickupFailReason;

import java.util.Map;

public interface PickupAttemptService {

    Map<String, Object> recordPickupAttempt(Integer orderId, Integer shipperId, PickupAttemptStatus status, PickupFailReason failReason, String note, String proofImageUrl);
}
