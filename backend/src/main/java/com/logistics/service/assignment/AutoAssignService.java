package com.logistics.service.assignment;

import com.logistics.entity.User;

import java.util.Optional;

public interface AutoAssignService {
    Optional<User> autoAssignOnArrival(Integer orderId);
    Optional<User> autoAssignPickupRequest(Integer orderId);
}
